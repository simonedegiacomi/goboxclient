package it.simonedegiacomi.storage;

import com.google.gson.JsonElement;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sun.net.httpserver.HttpExchange;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSException;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import it.simonedegiacomi.storage.components.HttpRequest;
import it.simonedegiacomi.storage.direct.HttpsStorageServer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Storage works like a server, saving the incoming file from the other clients
 * and sending it to other client (of the same GoBox account).
 *
 * @author Degiacomi Simone
 * Created on 24/12/2015.
 */
public class Storage {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(Storage.class.getName());

    /**
     * Default database location
     */
    private static final String DEFAULT_DB_LOCATION = "./config/db";

    /**
     * Default direct connection port
     */
    private static final int DIRECT_CONNECTION_DEFAULT_PORT = 6522;

    /**
     * Reference to the configuration
     */
    private final static Config config = Config.getInstance();

    /**
     * The environment is a singleton class that contains the object used by the storage
     */
    private final StorageEnvironment env;

    /**
     * URLBuilder is used to get the appropriate url
     */
    private final static URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * WebSocket communication with the main server
     */
    private MyWSClient mainServer;

    /**
     * Disconnect listener
     */
    private DisconnectedListener disconnectedListener;

    /**
     * Https server
     */
    private final HttpsStorageServer httpServer;

    /**
     * Srt of attached components
     */
    private final Set<GBModule> components = new HashSet<>();

    /**
     * Create a new storage given the Auth object for the appropriate account.
     * @param auth Authentication to use with the main server
     * @throws StorageException
     */
    public Storage (GoBoxEnvironment simpleEnv) throws StorageException, SQLException {
        this.env = new StorageEnvironment(simpleEnv);
        GBAuth auth = env.getAuth();

        // Connect to the local database
        initDatabase();

        try {

            // Create the web socket
            mainServer = new MyWSClient(urls.getURI("socketStorage"));
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot connect to the main server");
        }

        // Set the listener for the error event
        mainServer.onEvent("error", (data) -> {
            log.warn("websocket error");
            disconnectedListener.onDisconnected();
        });

        mainServer.onEvent("close", (data) -> {

            log.warn("websocket disconnected");
            disconnectedListener.onDisconnected();
        });

        // Authorize the ws connection
        auth.authorize(mainServer);

        // Create the event emitter
        this.env.setEmitter(new EventEmitter(mainServer));

        // Create the http(s) storage server that is used for direct transfers
        int directPort = Integer.parseInt(config.getProperty("directConnectionPort", String.valueOf(DIRECT_CONNECTION_DEFAULT_PORT)));
        String strAddress = config.getProperty("directConnectionListenAddress", "0.0.0.0");

        // Create the inet address (the broadcast
        InetSocketAddress address = new InetSocketAddress(strAddress, directPort);

        // Create the https server
        httpServer = new HttpsStorageServer(address);
        mainServer.addQueryHandler(httpServer.getWSQueryHandler());

        // Create a new internal client and set it in the environment
        env.setClient(new InternalClient(env));
        simpleEnv.setClient(env.getClient());

        // Load all the components
        loadComponents();
    }

    private void initDatabase () throws SQLException {
        ConnectionSource connectionSource = new JdbcConnectionSource("jdbc:h2:" + config.getProperty("database", DEFAULT_DB_LOCATION));
        env.setDbConnection(connectionSource);
        // Create the file table
        TableUtils.createTableIfNotExists(connectionSource, GBFile.class);

        // Create the event table
        TableUtils.createTableIfNotExists(connectionSource, SyncEvent.class);

        // Create the sharing table
        TableUtils.createTableIfNotExists(connectionSource, Sharing.class);

        Dao<GBFile, Long> fileTable = DaoManager.createDao(connectionSource, GBFile.class);

        // Check if the root file is already in the database
        if(fileTable.queryForId(GBFile.ROOT_ID) == null) {
            fileTable.create(GBFile.ROOT_FILE);
        }
    }

    /**
     * Start listening and serving.
     * @throws StorageException
     */
    public void startStoraging () throws StorageException {
        if(mainServer.isConnected())
            throw new IllegalStateException("Storage already initialized");
        try {

            // Open the connection and start to listen
            mainServer.connect();
        } catch (WSException ex) {

            throw new StorageException("Cannot connect to main server");
        }

        // Start the http server
        httpServer.serve();
        log.info("Storage started");
    }


    private void loadComponents () {

        // Create the service loader
        ServiceLoader<GBModule> loader = ServiceLoader.load(GBModule.class);
        log.info("GBComponents list loaded");

        // Iterate each component
        for (GBModule component : loader) {

            log.info("Analyzing component " + component.getClass().getName());

            // Get the class of the component
            Class componentClass = component.getClass();

            // Get all the methods of the class
            Method[] methods = componentClass.getMethods();

            // Analyze every method
            for (Method method : methods) {

                // Check if this method is a query listener
                if (method.getParameterCount() == 1 && method.getParameters()[0].getType().equals(JsonElement.class)) {

                    log.info("Found " + method.getName() + " (query handler)");

                    // Find the query name
                    WSQuery annotation = method.getAnnotation(WSQuery.class);

                    // Attach the query handler
                    mainServer.onQuery(annotation.name(), (data) -> {
                        try {
                            return (JsonElement) method.invoke(component, data);
                        } catch (IllegalAccessException ex) {
                            log.warn("Method in GBModule with wrong access restriction", ex);
                        } catch (InvocationTargetException ex) {
                            log.warn("GBModule method invocation exception", ex);
                        } catch (Exception ex) {
                            log.warn(ex.toString(), ex);
                        }
                        return null;
                    });

                    continue;
                }

                // check if this method is a http handler
                if (method.getParameterCount() == 1 && method.getParameters()[0].getType().equals(HttpExchange.class)) {

                    log.info("Found " + method.getName() + " (http handler)");

                    // Find the http method and name
                    HttpRequest annotation = method.getAnnotation(HttpRequest.class);

                    httpServer.addHandler(annotation.method(), annotation.name(), (httpExchange) -> {
                        try {
                            method.invoke(component, httpExchange);
                        } catch (IllegalAccessException ex) {
                            log.warn("Method in GBModule with wrong access restriction", ex);
                        } catch (InvocationTargetException ex) {
                            log.warn("GBModule method invocation exception", ex);
                        }
                    });
                }
            }

            // Call the attach method
            try {
                component.onAttach(env, new ComponentConfig(config));

                // Add the component to the components list
                components.add(component);
            } catch (AttachFailException ex) {
                log.warn("Method attaching failed", ex);

                // TODO: implement handlers removal
            }
        }

    }

    public interface DisconnectedListener {
        public void onDisconnected ();
    }

    public void onDisconnected(DisconnectedListener listener) {
        this.disconnectedListener = listener;
    }

    /**
     * Stop the storage
     */
    public void shutdown () {

        // Detach all the components
        components.forEach(GBModule::onDetach);

        httpServer.shutdown();

        // Disconnect from the main server
        mainServer.disconnect();
    }

    /**
     * Get the storage environment
     * @return Storage Environment
     */
    public StorageEnvironment getEnvironment () {
        return env;
    }
}