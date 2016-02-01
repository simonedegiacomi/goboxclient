package it.simonedegiacomi.goboxapi.myws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an implementation of web socket based on
 * the library 'TooTallNate/Java-WebSocket'. This
 * class add some feature like event handling and query
 * concept.
 *
 * Created by Degiacomi Simone on 28/12/15.
 */
public class MyWSClient {

    /**
     * ogger of the class
     */
    private static final Logger log = Logger.getLogger(MyWSClient.class.getName());

    /**
     * Websocket connection
     */
    private final WebSocketClient server;

    /**
     * Status of the connection
     */
    private boolean connected = false;

    /**
     * Map that contains the events listener. A event is formed by a event
     * name and his data. The listener is called when a event with a speicified
     * name is received
     * The association:
     * Name of the event => Listener for this event
     */
    private final HashMap<String, WSEventListener> events;

    /**
     * Map that contains the listeners of query received. Do not
     * confuse this map with the 'queryResponses':  that map contains
     * the listener for the RESPONSE of the query MADE, not RECEIVED.
     * Association:
     * Name of the query => Listener that answer this query
     */
    private final HashMap<String, WSQueryAnswer> queryAnswers;

    /**
     * This map contains the listener for the pending request made, Do not
     * confuse this map with the 'queryAnswer': The QueryAnswer ANSWER the
     * incoming query.
     * Association:
     * Query id of a made query => Response listener fot this query
     */
    private final HashMap<String, WSQueryResponseListener> queryResponses;

    /**
     * Executor used to use the java FutureTask
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    /**
     * This statis method allows you to set a proxy that will be used
     * for the new instances of this class
     * @param ip IP of the proxy
     * @param port Port of the proxy
     */
    public static void setProxy (String ip, String port) {

    }

    /**
     * Create a new client without connecting to the sever
     * @param uri URI of the server
     * @throws Exception
     */
    public MyWSClient (URI uri) throws Exception {

        // Initialize the maps
        events = new HashMap<>();
        queryAnswers = new HashMap<>();
        queryResponses = new HashMap<>();

        // Create the 'real' websocket client
        server = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                log.info("Websocket connection established");

                connected = true;

                // If there is an event listener for the open event, call it
                WSEventListener open = events.get("open");
                if (open != null)
                    open.onEvent(null);
            }

            @Override
            public void onMessage(String message) {
                try {

                    // Parse the message
                    JSONObject json = new JSONObject(message);

                    // get the _queryId
                    String queryId = json.optString("_queryId");

                    // If the message has not the queryId parameter
                    // is an simple event
                    if(queryId == null || queryId.length() <= 0) {
                        if(events.get(json.getString("event")) == null)
                            return;
                        events.get(json.getString("event"))
                                .onEvent(json.getJSONObject("data"));
                        return ;
                    }

                    // Now, check if is a queryResponse
                    // If is a query response i MUST have an listener on the
                    // 'queryResponse' map, so check here:
                    if(json.getString("event").equals("queryResponse")) {
                        // Get and remove the response listener
                        queryResponses.remove(queryId).onResponse(json.getJSONObject("data"));
                        return ;
                    }

                    // If is not a query response neither, is a query made to this program, so
                    // find the object that will answer this query.
                    JSONObject answer = queryAnswers.get(json.getString("event"))
                            .onQuery(json.getJSONObject("data"));

                    // When the answer is retrived, sendEvent back with the same
                    // queryId
                    JSONObject response = new JSONObject();
                    response.put("event", "queryResponse");
                    response.put("data", answer);
                    response.put("_queryId", json.getString("_queryId"));
                    response.put("forServer", false);
                    response.put("broadcast", false);
                    server.send(response.toString());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.toString(), ex);
                    log.warning("An incoming message from the websocket cannot be read: " + message);
                    System.out.println(message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connected = false;
                log.info("Websocket connection closed by the " + (remote ? "server" : "client"));
                WSEventListener close = events.get("close");
                if (close != null)
                    close.onEvent(null);
            }

            @Override
            public void onError(Exception ex) {
                log.log(Level.SEVERE, ex.toString(), ex);
                WSEventListener error = events.get("error");
                if (error != null)
                    error.onEvent(null);
            }
        };
    }

    /**
     * Start the connection to the server
     */
    public void connect() {
        server.connect();
    }

    /**
     * Start the connection blocking the thread until the
     * socket is connected (synchronized operation)
     */
    public void connectSync () throws InterruptedException {
        server.connectBlocking();
    }

    /**
     * This mathod allows you to register a new event listener
     * @param event Name of the event.
     * @param listener Listener of this event
     */
    public void on (String event, WSEventListener listener) {
        events.put(event, listener);
    }

    /**
     * This method allows you to register an method that answer a response
     * @param queryName Name of the query that the method will answer
     * @param listener Listener that will call to answer the query
     */
    public void onQuery (String queryName, WSQueryAnswer listener) {
        queryAnswers.put(queryName, listener);
    }

    /**
     * Make a new Query.
     * @param queryName Name of the query
     * @param query Data of the query
     * @param responseListener Listener that will call when the response of the query
     *                         is retrived.
     * @throws JSONException Exception in case that the request is not correct (just
     *                          make sure you don't insert weird things in data)
     */
    public void makeQuery (String queryName, JSONObject query, WSQueryResponseListener responseListener) {
        JSONObject json = new JSONObject();
        String queryId = String.valueOf(System.currentTimeMillis());
        try {
            json.put("event", queryName);
            json.put("forServer", false);
            json.put("data", query);
            json.put("_queryId", queryId);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        queryResponses.put(queryId, responseListener);
        server.send(json.toString());
    }

    /**
     * Same ad make query, but this will return immediately a futureTask.
     * @param queryName Name of the query
     * @param query Parameters of the quey
     * @return FutureTask, completed when the response  retriver
     */
    public FutureTask<JSONObject> makeQuery (String queryName, JSONObject query) {

        // Create a new wscallable
        final WSCallable callback = new WSCallable();

        // Create a enw FutureTask, that will be used to synchronize the incomng
        // response
        final FutureTask<JSONObject> future = new FutureTask<>(callback);

        // Make a normal query
        makeQuery(queryName, query, new WSQueryResponseListener() {
            @Override
            public void onResponse(JSONObject response) {

                // And when the result is retriver, set the response to the
                // callable
                callback.setResponse(response);

                // And execute the future task
                executor.execute(future);
            }
        });

        // Return the new future task
        return future;
    }

    /**
     * Send a new event
     * @param event Name of the event
     * @param data Data that will be sended with the event
     * @param forServer Specify if this should be catched from the server
     * @throws JSONException Trust me, this will never be trowed
     */
    public void sendEvent(String event, JSONObject data, boolean forServer) {
        JSONObject json = new JSONObject();
        try {
            json.put("event", event);
            json.put("forServer", forServer);
            json.put("broadcast", false);
            json.put("data", data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        server.send(json.toString());
    }

    /**
     * Send an event, but specify that is for all the clients
     * @param event Event name
     * @param data Event data
     */
    public void sendEventBroadcast (String event, JSONObject data) {
        JSONObject json = new JSONObject();
        try {
            json.put("event", event);
            json.put("forServer", false);
            json.put("broadcast", true);
            json.put("data", data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.fine("New broadcast message sent");
        server.send(json.toString());
    }

    /**
     * Remove a event listener
     * @param syncEvent Name of the event listener to remove
     */
    public void removeListener(String syncEvent) {
        events.remove(syncEvent);
    }

    /**
     * Return the state of the connection
     * @return State of the connection
     */
    public boolean isConnected() {
        return connected;
    }
}