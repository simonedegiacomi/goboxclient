package MyWS;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;

/**
 * Created by Degiacomi Simone on 28/12/15.
 */
public class MyWSClient {

    private final WebSocketClient server;
    private final HashMap<String, WSEvent> events;

    public MyWSClient (URI uri) throws Exception {
        events = new HashMap<>();

        server = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                WSEvent open = events.get("open");
                System.out.println("Open listener:" + open);
                if (open != null)
                    open.onEvent(null);
            }

            @Override
            public void onMessage(String message) {
                System.out.println(message);
                try {
                    // Parse the message
                    JSONObject json = new JSONObject(message);
                    events.get(json.getString("event"))
                            .onEvent(json.getJSONObject("data"));
                } catch (Exception ex) {
                    System.out.println("Error parsing:" + message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println(code);
                System.out.println(reason);
                System.out.println(remote);
                WSEvent close = events.get("close");
                if (close != null)
                    close.onEvent(null);
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
                WSEvent error = events.get("error");
                if (error != null)
                    error.onEvent(null);
            }
        };
        server.connect();
    }

    public void on (String event, WSEvent listener) {
        events.put(event, listener);
    }

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
