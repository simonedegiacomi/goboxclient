package it.simonedegiacomi.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.handlers.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage works like a server, saving the incoming
 * file from the other clients and sending it to
 * other client (of the same GoBox account).
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
     * Reference to the configuration
     */
    private final Config config = Config.getInstance();

    /**
     * URLBuilder is used to get the appropriate
     * url
     */
    private final URLBuilder urls = config.getUrls();

    /**
     * WebSocket communication with the main server
     */
    private MyWSClient mainServer;

    /**
     * Database used to store the files and the events.
     */
    private final StorageDB db;

    /**
     * Object used to notify the other clients
     */
    private EventEmitter emitter;

    private InternalClient internalClient;

    private final Gson gson = new Gson();

    /**
     * Create a new storage given the Auth object for the appropriate account.
     * @param auth Authentication to use with the main server
     * @throws StorageException
     */
    public Storage(final Auth auth) throws StorageException {

        // Connect to the local database
        try {
            this.db = new StorageDB("./config/db");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            throw new StorageException("Cannot connect to the local database");
        }

        // Try to connect ot the main server to check if the
        // token in the config is still valid
        try {
            JsonObject response = (JsonObject) EasyHttps.post(urls.get("authCheck"),
                    null, auth.getToken());
            if (!response.get("state").getAsString().equals("valid"))
                throw  new StorageException("Invalid token.");
            // Save the new token in the config
            auth.setToken(response.get("newOne").getAsString());
            config.setProperty("token", response.get("newOne").getAsString());
            config.save();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            throw new StorageException("Cannot verify the identity of the token");
        }

        // Now the token is valid, so we can connect to the main server through ws
        try {
            mainServer = new MyWSClient(urls.getURI("socketStorage"));
        } catch (Exception ex) {
            throw new StorageException("Cannot connect to the main server through handlers socket");
        }

        // Create the event emitter
        emitter = new EventEmitter(mainServer);

        // Set the listener for the open event
        mainServer.onEvent("open", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                mainServer.sendEvent("authentication", gson.toJsonTree(auth, Auth.class), true);

                // When the connection is established and the authentication
                // object sent, assign the events
                assignEvent();
            }
        });
    }

    public void startStoraging () throws Exception {
        // Open the connection and start to listen
        mainServer.connectSync();
    }

    /**
     * Assign the events listener for the incoming events
     * and incoming query from the main server and the other
     * clients trough handlers sockets.
     */
    private void assignEvent () {

        // Handler for the file list query
        mainServer.addQueryHandler(new FileInfoHandler(db));

        // Handler that create new directory
        mainServer.addQueryHandler(new CreateFolderHandler(db, emitter, internalClient));

        // Handler that send a file to a client
        mainServer.addEventHandler(new StorageToClientHandler(db));

        // Handler that receive the incoming file from a client
        mainServer.addEventHandler(new ClientToStorageHandler(db, emitter, internalClient));

        // Handler that remove files
        mainServer.addQueryHandler(new RemoveFileHandler(db, emitter, internalClient));

        // Handler that copy or cut files
        mainServer.addQueryHandler(new CopyOrCutHandler(db, emitter, internalClient));

        // Search handler
        mainServer.addQueryHandler(new SearchHandler(db));

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
        return (internalClient = internalClient == null ? new InternalClient(this, db) : internalClient);
    }

    protected EventEmitter getEventEmitter () {
        return emitter;
    }
}