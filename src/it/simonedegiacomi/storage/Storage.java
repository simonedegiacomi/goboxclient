package it.simonedegiacomi.storage;

import com.google.common.io.ByteStreams;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryAnswer;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage works like a server, saving the incoming
 * file from the other clients and sending it to
 * other client (of the same GoBox account).
 *
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class Storage {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(Storage.class.getName());

    /**
     * Reference to the it.simonedegiacomi.configuration
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

    /**
     * Listener of the internal client.
     * I know, the name is long, but is a clean name
     */
    private SyncEventListener internalSyncEventListener;

    /**
     * Create a new it.simonedegiacomi.storage given the Auth object for
     * the appropriate account.
     * @param auth Authentication to use with the main
     *             server
     * @throws StorageException
     */
    public Storage(Auth auth) throws StorageException {

        // Connect to the local databaase
        try {
            this.db = new StorageDB("config/gobox.db");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            throw new StorageException("Cannot connect to the local database");
        }

        // Try to connect ot the main server to check if the
        // token in the config is still valid
        try {
            JSONObject response = EasyHttps.post(urls.get("authCheck"),
                    null, auth.getToken());
            if (!response.getString("state").equals("valid"))
                throw  new StorageException("Invalid token.");
            // Save the new token in the config
            auth.setToken(response.getString("newOne"));
            config.setProperty("token", response.getString("newOne"));
            config.save();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            throw new StorageException("Cannot verify the identity of the token");
        }

        // Now the token is valid, so we can connect to the main server through ws
        try {
            mainServer = new MyWSClient(urls.getURI("socketStorage"));
        } catch (Exception ex) {
            throw new StorageException("Cannot connect to the main server through web socket");
        }

        // Set the listener for the open event
        mainServer.on("open", new WSEventListener() {
            @Override
            public void onEvent(JSONObject data) {
                mainServer.sendEvent("authentication", auth.toJSON(), true);

                // When the connection is established and the authentication
                // object sent, assign the events
                assignEvent();
            }
        });

        // Open the connection and start to listen
        mainServer.connect();
    }

    /**
     * Assign the evnts listener for the incoming events
     * and incoming query from the main server and the other
     * clients trough web sockets.
     */
    private void assignEvent () {

        // List the file inside a directory
        mainServer.onQuery("listFile", new WSQueryAnswer() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("ListFile query");
                try {
                    GBFile father = new GBFile(data);
                    JSONObject response = father.toJSON();
                    GBFile files[] = db.getChildrenByFather(father);
                    JSONArray jsonFiles = new JSONArray();
                    if (files != null)
                        for(GBFile file : files)
                            jsonFiles.put(file.toJSON());
                    response.put("children", jsonFiles);
                    return response;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    return null;
                }
            }
        });

        // Query listener for the create folder
        mainServer.onQuery("createFolder", new WSQueryAnswer() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("CreateFolder query");
                JSONObject response = new JSONObject();
                try {
                    // Re-create the new folder from the json request
                    GBFile newFolder = new GBFile(data);

                    // Insert the file and get the event
                    SyncEvent event = db.insertFile(newFolder);

                    // Create the real file in the FS
                    Files.createDirectory(newFolder.toPath(PATH));

                    // Then complete the response

                    response.put("newFolderId", newFolder.getID());
                    response.put("created", true);

                    // But first, send a broadcast message to advise the other
                    // client that a new folder is created

                    // The notification will contain the new file informations
                    emitEvent(event);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                }

                // Finally return the response
                return response;
            }
        });

        // Event that indicates that a client wants to upload a file.
        // i need to make an http request to the main server to get the file
        mainServer.on("comeToGetTheFile", new WSEventListener() {
            @Override
            public void onEvent(JSONObject data) {
                log.info("New come to get upload request");
                try {

                    // Get the uploadKey
                    String uploadKey = data.getString("uploadKey");

                    // Wrap the incoming file
                    GBFile incomingFile = new GBFile(data);

                    // Make the https request to the main server
                    URL url = new URL("https://goboxserver-simonedegiacomi.c9users.io/api/transfer/fromClient");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    // Abilitate input and output fro this request
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    // Create the json that will identificate the upload
                    JSONObject json = new JSONObject();
                    json.put("uploadKey", uploadKey);

                    // Send this json
                    // specify the length of the json
                    conn.setRequestProperty("Content-Length", String.valueOf(json.toString().length()));
                    PrintWriter out = new PrintWriter(conn.getOutputStream());
                    out.println(json.toString());
                    // Close the output stream of the request
                    out.close();

                    // get the file
                    int response = conn.getResponseCode();

                    // Create the stream to the disk
                    DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(incomingFile.getPathAsString(PATH)));

                    // Copy the stream
                    ByteStreams.copy(conn.getInputStream(), toDisk);

                    // Close file and http connection
                    toDisk.close();
                    conn.disconnect();

                    // Insert the file in the database
                    SyncEvent event = db.insertFile(incomingFile);

                    // The notification will contain the new file information
                    emitEvent(event);

                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });

        // Listener of the remove file event
        mainServer.on("removeFile", new WSEventListener() {

            @Override
            public void onEvent(JSONObject data) {
                try {
                    GBFile fileToRemove = new GBFile(data);
                    SyncEvent event = db.removeFile(fileToRemove);

                    // The notification will contain the deleted file
                    emitEvent(event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        mainServer.on("sendMeTheFile", new WSEventListener() {
            @Override
            public void onEvent(JSONObject data) {
                log.info("New download request");
                try {
                    String downloadKey = data.getString("downloadKey");
                    long file = data.getLong("id");

                    // get the file from the database
                    GBFile dbFile = db.getFileById(file);

                    URL url = urls.get("sendFileToClient");

                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);

                    DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());

                    // Open the file
                    DataInputStream fromFile = new DataInputStream(new FileInputStream(dbFile.getPathAsString()));

                    // Send the file
                    ByteStreams.copy(fromFile, toServer);

                    fromFile.close();
                    toServer.close();
                    conn.getResponseCode();
                    conn.disconnect();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    protected void emitEvent (SyncEvent eventToEmit) {
        mainServer.sendEventBroadcast("syncEvent", eventToEmit.toJSON());
        if (internalSyncEventListener != null)
            internalSyncEventListener.on(eventToEmit);
    }

    protected void setInternalSyncEventListener (SyncEventListener listener) {
        this.internalSyncEventListener = listener;
    }

    /**
     * Create a new InternalClient that can be used to
     * create a new Sync object. This client is used to
     * keep in sync the fs when the client is run in the
     * same machine of the it.simonedegiacomi.storage. It doesn't need to
     * communicate with the main server, and just talk with
     * the same database used by te it.simonedegiacomi.storage.
     *
     * @return
     */
    public Client getInternalClient () {

        // Create a new Internal Client that will use
        //the it.simonedegiacomi.storage database
        return new InternalClient(this, db);
    }
}