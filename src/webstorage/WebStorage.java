package webstorage;

import MyWS.MyWSClient;
import MyWS.WSEvent;
import MyWS.WSQuery;
import goboxstorage.Config;
import mydb.DBFile;
import mydb.MyDB;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.EasyHttps;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebStorage {

    private static final Logger log = Logger.getLogger(WebStorage.class.getName());

    private MyWSClient mainServer;
    private final Config config;
    private final MyDB db;
    private HashMap<String, EventListener> events;

    public WebStorage (Config config, MyDB db) {
        this.config = config;
        this.db = db;
        // Connect to the main server

        // Check if the token is still valid
        try {
            check();
            config.save();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
        }
        // Open the ws
        try {
            connect();
            System.out.println("WS connected");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
        }
    }

    /**
     * Logic to the server with the username in the config
     * and the specified password. This method block the
     * thread utnil the http call in complete. Once that
     * the response is retrived, the token is saved in the
     * config, othrerwise an exception is throwed.
     * @param config Config
     * @param password Password of the account
     * @throws Exception Exception
     */
    public static void login (Config config, char[] password) throws Exception {
        // Create the auth json
        JSONObject auth = new JSONObject();
        auth.put("username", config.getProperty("username"));
        auth.put("password", new String(password));
        auth.put("type", "S");
        // Make the request
        log.info("Checking credentials...");
        JSONObject response = EasyHttps.post(config.getProperty("SERVER_LOGIN"),
                auth, null);
        config.setProperty("token", response.getString("token"));
    }

    /**
     * Try to login with the token saved in the config
     */
    private void check () throws Exception {
        System.out.println("Check called with:" + config.getProperty("token"));
        JSONObject response = EasyHttps.post(config.getProperty("SERVER_CHECK"),
                null, config.getProperty("token"));
        if (response.getString("state").equals("valid"))
            config.setProperty("token", response.getString("newOne"));
        //else throw new Exception
        // TODO: Create this kind of exception, relative
        // to the account
    }

    /**
     * Connect to the main server via ws
     */
    public void connect () throws Exception {
        // Create the uri for the connection
        URI serverUri = new URI(config.getProperty("SERVER_WS"));
        mainServer = new MyWSClient(serverUri);
        mainServer.on("open", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                System.out.println("WS opened");
                try {
                    JSONObject auth = new JSONObject();
                    auth.put("token", config.getProperty("token"));
                    mainServer.send("authentication", auth, true);
                    System.out.println("Authentication sent");
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                }
                assignEvent();
            }
        });

        mainServer.on("close", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                System.out.println("WS -> Close");
            }
        });

        mainServer.on("error", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                System.out.println("WS error");
            }
        });
    }

    private void assignEvent () {

        mainServer.onQuery("listFile", new WSQuery() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("ListFile query");
                try {
                    long fatherId = data.getLong("father");
                    // add information about the folder
                    DBFile father = db.getFileById(fatherId);
                    JSONObject response = father.toJSON();
                    DBFile files[] = db.getChildrenByFather(fatherId);
                    JSONArray jsonFiles = new JSONArray();
                    if (files != null)
                        for(DBFile file : files)
                            jsonFiles.put(file.toJSON());
                    response.put("children", jsonFiles);
                    return response;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    return null;
                }
            }
        });

        mainServer.onQuery("createFolder", new WSQuery() {
            @Override
            public JSONObject onQuery(JSONObject data) {
                log.info("CreateFolder query");
                JSONObject response = new JSONObject();
                try {
                    DBFile newFolder = new DBFile(data.getString("name"), data.getLong("father"), true);
                    db.insertFile(newFolder);
                    response.put("newFolderId", newFolder.getID());
                    response.put("created", true);
                    return response;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    return null;
                }
            }
        });

        mainServer.on("comeToGetTheFile", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                log.info("New come to ceth upload request");
                try {
                    // Get the uploadKey
                    String uploadKey = data.getString("uploadKey");
                    // Get the name of the file
                    String fileName = data.getString("name");
                    // And the id of the folder that will contain the file
                    long father = data.getLong("father");
                    // Insert the file in the database
                    DBFile dbFile = new DBFile(fileName, father, false);
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
                    DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(String.valueOf(dbFile.getID())));
                    // Create a buffer to read the file
                    byte[] buffer = new byte[256];
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
                    DBFile dbFile = db.getFileById(file);
                    URL url = new URL("");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());
                    // Open the file
                    DataInputStream fromFile = new DataInputStream(new FileInputStream(""));
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
}