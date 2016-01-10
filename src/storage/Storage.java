package storage;

import com.google.common.io.ByteStreams;
import configuration.Config;
import goboxapi.GBFile;
import goboxapi.myws.MyWSClient;
import goboxapi.myws.WSEvent;
import goboxapi.myws.WSQueryAnswer;
import goboxapi.utils.URLBuilder;
import goboxapi.authentication.Auth;
import goboxapi.client.Client;
import goboxapi.client.SyncEvent;
import goboxapi.utils.EasyHttps;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class Storage {

    private static final Logger log = Logger.getLogger(Storage.class.getName());

    private final Config config = Config.getInstance();

    private final URLBuilder urls = config.getUrls();

    private MyWSClient mainServer;

    private final StorageDB db;
    private final String PATH = "files\\";

    public Storage(Auth auth) throws StorageException {
        try {
            this.db = new StorageDB("config/gobox.db");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            throw new StorageException("Cannot connect to the local database");
        }

        // Try to connect ot the main server to check if the
        // token in the config is still valid
        try {
            JSONObject response = EasyHttps.post("https://goboxserver-simonedegiacomi.c9users.io/api/user/check",
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
            mainServer = new MyWSClient(new URI("ws://goboxserver-simonedegiacomi.c9users.io/api/ws/storage"));
        } catch (Exception ex) {
            throw new StorageException("Cannot connect to the main server through web socket");
        }
        mainServer.on("open", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                mainServer.sendEvent("authentication", auth.toJSON(), true);
                // When the connection is established and the authentication
                // object sent, assign the events
                assignEvent();
            }
        });
    }

    private void assignEvent () {
        // List the file inside a directory
        mainServer.onQuery("listFile", new WSQueryAnswer() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("ListFile query");
                try {
                    long fatherId = data.getLong("father");
                    // add information about the folder
                    GBFile father = db.getFileById(fatherId);
                    JSONObject response = father.toJSON();
                    GBFile files[] = db.getChildrenByFather(fatherId);
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

        // Create new folder event
        mainServer.onQuery("createFolder", new WSQueryAnswer() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("CreateFolder query");
                JSONObject response = new JSONObject();
                try {
                    GBFile newFolder = new GBFile(data);
                    // Insert the file and get the event
                    SyncEvent event = db.insertFile(newFolder);

                    // Create the real file
                    Files.createDirectory(newFolder.toPath());

                    // Then complete the response

                    response.put("newFolderId", newFolder.getID());
                    response.put("created", true);

                    // But first, send a broadcast message to advise the other
                    // client that a new folder is created

                    // The notification will contain the new file informations
                    mainServer.sendEventBroadcast("newFile", event.toJSON());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                }

                // Finally return the response
                return response;
            }
        });

        // Event that indicates that a client wants to upload a file.
        // i need to make an http request to the main server to get the file
        mainServer.on("comeToGetTheFile", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                log.info("New come to ceth upload request");
                try {

                    // Get the uploadKey
                    String uploadKey = data.getString("uploadKey");

                    // Get the name of the file
                    String fileName = data.getString("name");

                    // The id of the folder that will contain the file
                    long father = data.getLong("father");

                    // Insert the file in the database
                    GBFile dbFile = new GBFile(fileName, father, false);

                    // And set the other informations
                    dbFile.setSize(data.getLong("size"));

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
                    DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(dbFile.getPath()));

                    // Copy the stream
                    ByteStreams.copy(conn.getInputStream(), toDisk);

                    // Close file and http connection
                    toDisk.close();
                    conn.disconnect();

                    // Insert the file in the database
                    SyncEvent event = db.insertFile(dbFile);

                    // The notification will contain the new file informations
                    mainServer.sendEventBroadcast("newFile", event.toJSON());

                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });

        mainServer.on("removeFile", new WSEvent() {

            @Override
            public void onEvent(JSONObject data) {
                try {
                    GBFile fileToRemove = new GBFile(data);
                    SyncEvent event = db.removeFile(fileToRemove);

                    // The notification will contain the deleted file
                    mainServer.sendEventBroadcast("removeFile", event.toJSON());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        mainServer.on("sendMeTheFile", new WSEvent() {
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
                    DataInputStream fromFile = new DataInputStream(new FileInputStream(dbFile.getPath()));

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

    public Client getInternalClient () {
        return new InternalClient(db);
    }
}