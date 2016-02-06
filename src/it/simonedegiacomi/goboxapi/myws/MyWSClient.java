package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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
     * Proxy to use to connect to the web socket servers
     */
    private static InetSocketAddress proxy;

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
     * This static method allows you to set a proxy that will be used
     * for the new instances of this class
     * @param ip IP of the proxy
     * @param port Port of the proxy
     */
    public static void setProxy (String ip, int port) {
        proxy = new InetSocketAddress(ip, port);
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

        final JsonParser parser = new JsonParser();

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
                    JsonObject json = (JsonObject) parser.parse(message);


                    // get the _queryId
                    String queryId = json.get("_queryId").getAsString();

                    // If the message has not the queryId parameter
                    // is an simple event
                    if(queryId == null || queryId.length() <= 0) {
                        if(events.get(json.get("event").getAsString()) == null)
                            return;
                        events.get(json.get("event").getAsString())
                                .onEvent(json.get("data"));
                        return ;
                    }

                    // Now, check if is a query response
                    // If is a query response i MUST have an listener on the
                    // 'queryResponse' map, so check here:
                    if(json.get("event").getAsString().equals("queryResponse")) {
                        // Get and remove the response listener
                        queryResponses.remove(queryId).onResponse(json.get("data"));
                        return ;
                    }

                    // If is not a query response neither, is a query made to this program, so
                    // find the object that will answer this query.
                    JsonElement answer = queryAnswers.get(json.get("event").getAsString())
                            .onQuery(json.get("data"));

                    // When the answer is retrive, sendEvent back with the same
                    // queryId
                    JsonObject response = new JsonObject();
                    response.addProperty("event", "queryResponse");
                    response.add("data", answer);
                    response.addProperty("_queryId", json.get("_queryId").getAsString());
                    response.addProperty("forServer", false);
                    response.addProperty("broadcast", false);
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
        if(proxy != null)
            server.setProxy(proxy);
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
     * This method allows you to register a new event listener
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
     *                         is retrieve.
     */
    public void makeQuery (String queryName, JsonElement query, WSQueryResponseListener responseListener) {
        JsonObject json = new JsonObject();
        String queryId = String.valueOf(System.currentTimeMillis());
        try {
            json.addProperty("event", queryName);
            json.addProperty("forServer", false);
            json.add("data", query);
            json.addProperty("_queryId", queryId);
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
    public FutureTask<JsonElement> makeQuery (String queryName, JsonElement query) {

        // Create a new wscallable
        final WSCallable callback = new WSCallable();

        // Create a new FutureTask, that will be used to synchronize the incoming
        // response
        final FutureTask<JsonElement> future = new FutureTask<>(callback);

        // Make a normal query
        makeQuery(queryName, query, new WSQueryResponseListener() {
            @Override
            public void onResponse(JsonElement response) {

                // And when the result is retrieved, set the response to the
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
     * @param forServer Specify if this should be catch from the server
     */
    public void sendEvent(String event, JsonElement data, boolean forServer) {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("event", event);
            json.addProperty("forServer", forServer);
            json.addProperty("broadcast", false);
            json.add("data", data);
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
    public void sendEventBroadcast (String event, JsonElement data) {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("event", event);
            json.addProperty("forServer", false);
            json.addProperty("broadcast", true);
            json.add("data", data);
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
