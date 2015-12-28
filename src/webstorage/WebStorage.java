package webstorage;

import MyWS.MyWSClient;
import MyWS.WSEvent;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class WebStorage {

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
            ex.printStackTrace();
            System.out.println("Check token failed");
        }
        System.out.println("Check complete");
        // Open the ws
        try {
            connect();
            System.out.println("WS connected");
        } catch (Exception ex) {
            System.out.println("Connection failed");
            ex.printStackTrace();
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
                    ex.printStackTrace();
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

        mainServer.on("listFile", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                try {
                    /**
                     * {
                     *     _querId: number,
                     *     children: [
                     *          file,
                     *          file,
                     *     ]
                     * }
                     */
                    DBFile files[] = db.getChildrenByFather(data.getLong("father"));
                    JSONObject response = new JSONObject();
                    response.put("_queryId", data.getString("_queryId"));
                    JSONArray jsonFiles = new JSONArray();
                    if (files != null)
                        for(DBFile file : files)
                            jsonFiles.put(file.toJSON());
                    response.put("children", jsonFiles);
                    mainServer.send("queryResult", response, false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        System.out.println("Assign event");
    }
}