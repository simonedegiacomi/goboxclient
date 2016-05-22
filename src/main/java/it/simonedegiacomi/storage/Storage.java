package it.simonedegiacomi.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSException;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.direct.HttpsStorageServer;
import it.simonedegiacomi.storage.direct.UDPStorageServer;
import it.simonedegiacomi.storage.handlers.ws.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

    private final String PATH = config.getProperty("path", "files/");

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

    private DisconnectedListener disconnectedListener;

    public Storage (GBAuth auth) throws StorageException {
        this(auth, new StorageEnvironment());
    }

    /**
     * Create a new storage given the Auth object for the appropriate account.
     * @param auth Authentication to use with the main server
     * @throws StorageException
     */
    public Storage (final GBAuth auth, StorageEnvironment env) throws StorageException {
        this.env = env;

        // Connect to the local database
        env.setDB(new DAOStorageDB(DEFAULT_DB_LOCATION));

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
        if (env.getEmitter() == null ) {
            env.setEmitter(new EventEmitter(mainServer));
        }

        try {
            // Create the local UDP server
            UDPStorageServer udp = new UDPStorageServer(Integer.parseInt(config.getProperty("udpDirectConnectionPort", String.valueOf(UDPStorageServer.DEFAULT_PORT))));
            udp.init();
            env.setUdpServer(udp);
        } catch (UnknownHostException ex) {
            log.warn(ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString());
        }


        // Create the http(s) storage server that is used for direct transfers
        int directPort = Integer.parseInt(config.getProperty("directConnectionPort", String.valueOf(DIRECT_CONNECTION_DEFAULT_PORT)));
        String strAddress = config.getProperty("directConnectionListenAddress", "0.0.0.0");

        try {

            // Create the inet address (the broadcast
            InetSocketAddress address = new InetSocketAddress(strAddress, directPort);

            // Create the https server
            HttpsStorageServer https = new HttpsStorageServer(address, env);
            https.init();
            env.setHttpsServer(https);
        } catch (IOException ex) {
            log.warn(ex.toString());
        }

        // Create a new internal client and set it in the environment
        env.setInternalClient(new InternalClient(env));
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

        // Start the UDP server
        if(env.getUdpServer() != null)
            env.getUdpServer().start();

        // Start the local http(s) server
        if(env.getHttpsServer() != null)
            env.getHttpsServer().serve();

        // Assign event handler from the ws client
        assignEvent();

        log.info("Storage started");
    }

    /**
     * Assign the events listener for the incoming events and incoming query from the main
     * server and the other clients trough handlers sockets.
     */
    private void assignEvent () {

        // Handler for the file list query
        mainServer.addQueryHandler(new FileInfoHandler(env));

        // Handler that create new directory
        mainServer.addQueryHandler(new CreateFolderHandler(env));

        // Handler that send a file to a client
        mainServer.addQueryHandler(new StorageToClientHandler(env));

        // Handler that receive the incoming file from a client
        mainServer.addQueryHandler(new ClientToStorageHandler(env));

        // Handler for the trash
        TrashHandler trashHandler = new TrashHandler(env);
        mainServer.addQueryHandler(trashHandler.getDeleteHandler());
        mainServer.addQueryHandler(trashHandler.getTrashedHandler());
        mainServer.addQueryHandler(trashHandler.getTrashHandler());
        mainServer.addQueryHandler(trashHandler.getEmptyTrashHandler());

        // Rename handler
        mainServer.addQueryHandler(new RenameFileHandler(env));

        // Handler that copy or cut files
        mainServer.addQueryHandler(new MoveHandler(env));

        // Search handler
        mainServer.addQueryHandler(new SearchHandler(env));

        // Share handler
        mainServer.addQueryHandler(new ShareListHandler(env));
        mainServer.addQueryHandler(new ShareHandler(env));

        // Recent files
        mainServer.addQueryHandler(new RecentHandler(env));

        // The http server that manage direct transfers also has an integrated WSQueryHandler
        if (env.getHttpsServer() != null)
            mainServer.addQueryHandler(env.getHttpsServer().getWSComponent());

        // Add a simple ping handler
        mainServer.onQuery("ping", new WSQueryHandler() {
            @Override
            public JsonElement onQuery(JsonElement data) {
                return new JsonObject();
            }
        });
    }

    /**
     * Create a new InternalClient that can be used to create a new Sync object.
     * This client is used to keep in sync the fs when the client is run in the
     * same machine of the storage. It doesn't need to communicate with the main
     * server, and just talk with the same database used by the storage.
     *
     * @return The internal client instance.
     */
    public GBClient getInternalClient () {
        return env.getInternalClient();
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
        try {
            // Stop the udp server
            if(env.getUdpServer() != null)
                env.getUdpServer().shutdown();
        } catch (InterruptedException ex) {
            log.warn(ex.toString());
        }

        // Stop the https server
        if (env.getHttpsServer() != null)
            env.getHttpsServer().shutdown();

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