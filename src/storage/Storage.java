package storage;

import goboxapi.authentication.Auth;
import goboxapi.GBFile;
import goboxapi.MyWS.MyWSClient;
import goboxapi.MyWS.WSEvent;
import goboxapi.MyWS.WSQueryAnswer;
import configuration.Config;
import goboxapi.client.Client;
import org.json.JSONArray;
import org.json.JSONObject;
import goboxapi.utils.EasyHttps;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Storage {

    private static final Logger log = Logger.getLogger(Storage.class.getName());

    private MyWSClient mainServer;
    private final Config config;
    private final StorageDB db;
    private final String PATH = "files\\";

    public Storage(Auth auth) throws StorageException {
        this.config = Config.getInstance();

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
                    db.insertFile(newFolder);
                    // Create the real file
                    Files.createDirectory(newFolder.toPath());
                    response.put("newFolderId", newFolder.getID());
                    response.put("created", true);
                    return response;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    return null;
                }
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
                    db.insertFile(dbFile);

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
                    // Open the stream incoming from the server (that contains the file)
                    DataInputStream fromConnection = new DataInputStream(conn.getInputStream());

                    // Create the stream to the disk
                    DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(dbFile.getPath()));

                    // Create a buffer to read the file
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while((read = fromConnection.read(buffer)) > 0)
                        toDisk.write(buffer, 0, read);
                    // Close file and http connection
                    toDisk.close();
                    fromConnection.close();
                    conn.disconnect();
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
                    URL url = new URL("");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());
                    // Open the file
                    DataInputStream fromFile = new DataInputStream(new FileInputStream(dbFile.getPath()));
                    int read = 0;
                    byte[] buffer = new byte[256];
                    while((read = fromFile.read(buffer)) > 0)
                        toServer.write(buffer, 0, read);
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