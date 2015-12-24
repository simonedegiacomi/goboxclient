package webstorage;

import jdk.nashorn.internal.parser.JSONParser;
import mydb.MyDB;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class WebStorage {
    private WebSocketClient mainServer;
    private final Properties config;
    private final MyDB db;
    private HashMap<String, MyEventListener> events;


    public WebStorage (Properties config, MyDB db) {
        this.config = config;
        this.db = db;
        assignEvent();
    }

    /**
     * Connect to the main server via ws
     */
    public void connect () throws Exception {
        // Create the uri for the connection
        URI serverURI = new URI(config.getProperty("SERVER_WS"));
        mainServer = new WebSocketClient(serverURI) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                // Send the auth informations
                JSONObject auth = new JSONObject();
                try {
                    auth.put("ID", config.getProperty("USER_ID"));
                    auth.put("Token", config.getProperty(""));
                    mainServer.send(auth.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(String message) {
                // The message received is json, so let's
                // parse it
                try {
                    JSONObject json = new JSONObject(message);
                    events.get(json.getString("event")).onEvent(json);
                } catch (Exception ex) {
                    // emh... i need to handle this...
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                events.get("close").onEvent(null);
            }

            @Override
            public void onError(Exception ex) {
                events.get("error").onEvent(null);
            }
        };
    }

    private void assignEvent () {
        events = new HashMap<>();
        // Add the 'sendMeFile' event
        events.put("sendMeFile", new MyEventListener() {
            @Override
            public void onEvent(JSONObject data) {

            }
        });

        // Add the 'comeToCatchFile' event
        events.put("comeToCatchFile", new MyEventListener() {
            @Override
            public void onEvent(JSONObject data) {

            }
        });


    }

    public disconnectAndLogout (boolean logout) {
        if (logout)
            sendLogoutMessage();
        // Disconect
        mainServer.close();
    }

    private void sendLogoutMessage() {
        JSONObject logoutMessage = new JSONObject();
        try {
            logoutMessage.put("event", "logout");
            logoutMessage.put("forServer", true);
            mainServer.send(logoutMessage.toString());
        } catch (Exception ex) {

        }
    }
}
