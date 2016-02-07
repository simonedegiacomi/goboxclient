package it.simonedegiacomi.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.handlers.ClientToStorageHandler;
import it.simonedegiacomi.storage.handlers.FileListHandler;
import it.simonedegiacomi.storage.handlers.FindPathHandler;
import it.simonedegiacomi.storage.handlers.StorageToClientHandler;

import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage works like a server, saving the incoming
 * file from the other clients and sending it to
 * other client (of the same GoBox account).
 *
 * Created by Degiacomi Simone onEvent 24/12/2015.
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
     * Path of the files folder
     */
    private final String PATH = "files";

    private EventEmitter emitter;

    private final Auth auth;

    private InternalClient internalClient;

    private final Gson gson = new Gson();

    /**
     * Create a new storage given the Auth object for the appropriate account.
     * @param auth Authentication to use with the main server
     * @throws StorageException
     */
    public Storage(Auth auth) throws StorageException {

        this.auth = auth;

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

        mainServer.addQueryHandler(new FileListHandler(db));

        mainServer.addEventHandler(new ClientToStorageHandler(db));

        mainServer.addEventHandler(new StorageToClientHandler(db, emitter));

        mainServer.addQueryHandler(new FindPathHandler(db));

        // Query listener for the create folder
        mainServer.onQuery("createFolder", new WSQueryHandler() {
            @Override
            public JsonElement onQuery(JsonElement data) {
                log.info("CreateFolder query");
                JsonObject response = new JsonObject();
                try {
                    // Re-create the new folder from the json request
                    GBFile newFolder = gson.fromJson(data, GBFile.class);

                    // Create the real file in the FS
                    Files.createDirectory(newFolder.toFile(PATH).toPath());

                    // Insert the file and get the event
                    SyncEvent event = db.insertFile(newFolder);

                    // Then complete the response

                    response.addProperty("newFolderId", newFolder.getID());
                    response.addProperty("created", true);

                    notifyInternalClient(newFolder);
                    // But first, send a broadcast message to advise the other
                    // client that a new folder is created

                    // The notification will contain the new file information
                    emitter.emitEvent(event);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                }

                // Finally return the response
                return response;
            }
        });

        mainServer.onQuery("updateFile", new WSQueryHandler() {

            @Override
            public JsonElement onQuery (JsonElement data) {
                JsonObject response = new JsonObject();
                try {
                    GBFile fileToUpdate = gson.fromJson(data, GBFile.class);
                    SyncEvent event = db.updateFile(fileToUpdate);

                    emitter.emitEvent(event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return response;
            }
        });

        // Listener of the remove file event
        mainServer.onEvent("removeFile", new WSEventListener() {

            @Override
            public void onEvent(JsonElement data) {
                try {
                    GBFile fileToRemove = gson.fromJson(data, GBFile.class);
                    SyncEvent event = db.removeFile(fileToRemove);

                    // The notification will contain the deleted file
                    emitter.emitEvent(event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        mainServer.onQuery("ping", new WSQueryHandler() {
            @Override
            public JsonElement onQuery(JsonElement data) {
                return new JsonObject();
            }
        });

        mainServer.onQuery("getEventsList", new WSQueryHandler() {
            @Override
            public JsonElement onQuery(JsonElement data) {
                long lastHeardId = ((JsonObject) data).get("id").getAsLong();
                List<SyncEvent> events = db.getUniqueEventsFromID(lastHeardId);
                // Return the gson tree. To create this tree, i need to implement a new TypToken
                // and instantiate it. As you can see the implementation is empty, but doing that
                // i can get the type. (this because i can't use List<SyncEvent>.class . If you want to know
                // more google 'Type Erasure Java')
                return gson.toJsonTree(events, new TypeToken<List<SyncEvent>> () {}.getType());
            }
        });
    }

    private void notifyInternalClient(GBFile file) {
        if(internalClient != null)
            internalClient.ignore(file);
    }

    /**
     * Create a new InternalClient that can be used to
     * create a new Sync object. This client is used to
     * keep in sync the fs when the client is run in the
     * same machine of the storage. It doesn't need to
     * communicate with the main server, and just talk with
     * the same database used by the storage.
     *
     * @return
     */
    public Client getInternalClient () {
        return (internalClient = internalClient == null ? new InternalClient(this, db) : internalClient);
    }

    protected EventEmitter getEventEmitter () {
        return emitter;
    }
}