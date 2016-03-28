package it.simonedegiacomi.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.WebSocketException;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
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

    private static final String DEFAULT_DB_LOCATION = "./config/db";

    /**
     * Reference to the configuration
     */
    private final Config config = Config.getInstance();

    /**
     * URLBuilder is used to get the appropriate
     * url
     */
    private static URLBuilder urls;

    /**
     * WebSocket communication with the main server
     */
    private MyWSClient mainServer;

    private final StorageEnvironment env = new StorageEnvironment();

    private DisconnectedListener disconnectedListener;

    /**
     * Initialize setting the urls to use
     * @param builder URL builder
     */
    public static void setUrls (URLBuilder builder) {
        urls = builder;
    }

    /**
     * Create a new storage given the Auth object for the appropriate account.
     * @param auth Authentication to use with the main server
     * @throws StorageException
     */
    public Storage(final Auth auth) throws StorageException {

        // Connect to the local database
        env.setDB(new StorageDB(DEFAULT_DB_LOCATION));

        try {
            // Create the web socket
            mainServer = new MyWSClient(urls.getURI("socketStorage"));
        } catch (IOException ex) {
            throw new StorageException("Cannot connect to the main server");
        }

        // Set the listener for the error event
        mainServer.onEvent("error", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                disconnectedListener.onDisconnected();
            }
        });

        // Authorize the ws connection
        auth.authorizeWs(mainServer);

        // Create the event emitter
        env.setEmitter(new EventEmitter(mainServer));

        try {
            // Create the local UDP server
            env.setUdpServer(new UDPStorageServer(UDPStorageServer.DEFAULT_PORT));
        } catch (UnknownHostException ex) {
            log.warn(ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString());
        }

        try {
            // Create the http(s) storage server that is used for direct transfers
            int port = Integer.parseInt(config.getProperty("directConnectionPort"));
            InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);
            env.setHttpsServer(new HttpsStorageServer(address, env));
        } catch (IOException ex) {
            log.warn(ex.toString());
        }

        env.setInternalClient(new InternalClient(env));

        // Set all the others events and queries
        assignEvent();
    }

    public void startStoraging () throws StorageException {
        try {
            // Open the connection and start to listen
            mainServer.connect();
        } catch (WebSocketException ex) {
            throw new StorageException("Cannot connect to main server");
        }

        // Start the UDP server
        env.getUdpServer().start();

        // Start the local http(s) server
        //httpStorage.forwardPort();
        env.getHttpsServer().serve();
    }

    /**
     * Assign the events listener for the incoming events
     * and incoming query from the main server and the other
     * clients trough handlers sockets.
     */
    private void assignEvent () {

        // Handler for the file list query
        mainServer.addQueryHandler(new FileInfoHandler(env));

        // Handler that create new directory
        mainServer.addQueryHandler(new CreateFolderHandler(env));

        // Handler that send a file to a client
        mainServer.addEventHandler(new StorageToClientHandler(env));

        // Handler that receive the incoming file from a client
        mainServer.addEventHandler(new ClientToStorageHandler(env));

        // Handler that remove files
        mainServer.addQueryHandler(new RemoveFileHandler(env));

        // Handler that copy or cut files
        mainServer.addQueryHandler(new CopyOrCutHandler(env));

        // Search handler
        mainServer.addQueryHandler(new SearchHandler(env));

        // Share handler
        mainServer.addQueryHandler(new ShareListHandler(env));
        mainServer.addQueryHandler(new ShareHandler(env));

        // The http server that manage direct transfers also has an integrated WSQueryHandler
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
    public Client getInternalClient () {
        return env.getInternalClient();
    }

    public interface DisconnectedListener {
        public void onDisconnected ();
    }

    public void onDisconnected(DisconnectedListener listener) {
        this.disconnectedListener = listener;
    }

    public void shutdown () {
        try {
            env.getUdpServer().shutdown();
        } catch (InterruptedException ex) {
            log.warn(ex.toString());
        }
        env.getHttpsServer().shutdown();
    }
}