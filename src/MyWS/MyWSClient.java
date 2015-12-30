package MyWS;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Degiacomi Simone on 28/12/15.
 */
public class MyWSClient {

    private static final Logger log = Logger.getLogger(MyWSClient.class.getName());

    private final WebSocketClient server;
    private final HashMap<String, WSEvent> events;
    private final HashMap<String, WSQuery> queryListener;

    public MyWSClient (URI uri) throws Exception {
        events = new HashMap<>();
        queryListener = new HashMap<>();

        server = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                log.info("Websocket connection established");
                WSEvent open = events.get("open");
                if (open != null)
                    open.onEvent(null);
            }

            @Override
            public void onMessage(String message) {
                try {
                    // Parse the message
                    JSONObject json = new JSONObject(message);
                    // If the message has not the queryId parameter
                    // is an simple event
                    if(!json.has("_queryId"))
                        events.get(json.getString("event"))
                            .onEvent(json.getJSONObject("data"));

                    // If the message is a query, call the query listener
                    JSONObject answer = queryListener.get(json.getString("event"))
                            .onQuery(json.getJSONObject("data"));

                    // When the answer is retrived, send back with the same
                    // queryId
                    JSONObject response = new JSONObject();
                    response.put("event", "queryResponse");
                    response.put("data", answer);
                    response.put("_queryId", json.getString("_queryId"));
                    response.put("forserver", false);
                    response.put("broadcast", false);
                    server.send(response.toString());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    log.warning("An incoming message from the websocket cannot be read");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("Websocket connection closed by the " + (remote ? "server" : "client"));
                WSEvent close = events.get("close");
                if (close != null)
                    close.onEvent(null);
            }

            @Override
            public void onError(Exception ex) {
                log.log(Level.SEVERE, ex.toString(), ex);
                WSEvent error = events.get("error");
                if (error != null)
                    error.onEvent(null);
            }
        };
        server.connectBlocking();
    }

    public void on (String event, WSEvent listener) {
        events.put(event, listener);
    }

    public void onQuery (String queryName, WSQuery listener) {
        queryListener.put(queryName, listener);
    };

    public void send (String event, JSONObject data, boolean forServer) throws Exception {
        JSONObject json = new JSONObject();
        json.put("event", event);
        json.put("forServer", forServer);
        json.put("broadcast", false);
        json.put("data", data);
        System.out.println("Sent: " + json.toString());
        server.send(json.toString());
    }

    public void sendBroadcast (String event, JSONObject data) throws Exception {
        JSONObject json = new JSONObject();
        json.put("event", event);
        json.put("forServer", false);
        json.put("broadcast", true);
        json.put("data", data);
        System.out.println("Sent: " + json.toString());
        server.send(json.toString());
    }
}
