package it.simonedegiacomi.goboxapi.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryResponseListener;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an implementation of the gobox api client
 * interface. This client uses WebSocket to transfer
 * the file list, to authenticate and to share events
 * and use HTTP(s) to transfer the files.
 *
 * Created by Degiacomi Simone onEvent 31/12/2015.
 */
public class StandardClient implements Client {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(StandardClient.class.getName());

    /**
     * Object used to create the urls.
     * TODO: Get the url builder from the constructor
     */
    private final URLBuilder urls = Config.getInstance().getUrls();

    /**
     * WebSocket connection to the server
     */
    private MyWSClient server;

    /**
     * Authorization object used to make the call
     */
    private final Auth auth;

    private final Gson gson = new Gson();

    private final Set<String> eventsToIgnore = new HashSet<>();

    /**
     * Construct a sync object, but first try to login to gobox.
     * The constructor will block the thread util the authentication
     * is complete
     * @param auth Auth object that will be used to authenticate
     */
    public StandardClient(Auth auth) throws ClientException {

        this.auth = auth;

        // Connect to the server
        try {

            // Create the websocket client
            server = new MyWSClient(urls.getURI("socketClient"));

            // When the webSocket in opened, send the authentication object
            server.onEvent("open", new WSEventListener() {
                @Override
                public void onEvent(JsonElement data) {

                    log.fine("Authentication trough webSocket");

                    // Send the authentication object

                    // TODO: handle an authentication error
                    server.sendEvent("authentication", gson.toJsonTree(auth, Auth.class), true);
                }
            });

            // Register the storageInfo event
            server.onEvent("storageInfo", new WSEventListener() {
                @Override
                public void onEvent(JsonElement data) {
                    System.out.println(data);
                }
            });

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    public void connect () {
        // Start listening onEvent the websocket, connecting to the server
        server.connect();
    }

    /**
     * Download a file from the storage copying the file to the output stream
     *
     * @param file File to download.
     * @param dst Output stream where put the content of the file.
     * @throws ClientException
     */
    @Override
    public void getFile (GBFile file, OutputStream dst) throws ClientException {
        try {

            // Create and fill the request object
            JsonObject request = new JsonObject();
            request.addProperty("ID", file.getID());

            URL url = urls.get("getFile", request);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Authorize the connection
            auth.authorize(conn);

            // Copy the file
            ByteStreams.copy(conn.getInputStream(), dst);

            // Close the connection
            conn.disconnect();

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        try {
            // Ignore the events from the server related to this file
            eventsToIgnore.add(newDir.toFile().toString());
            FutureTask<JsonElement> future = server.makeQuery("createFolder", gson.toJsonTree(newDir, GBFile.class));
            JsonObject response = (JsonObject) future.get();
            newDir.setID(response.get("newFolderId").getAsLong());
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }


    @Override
    public boolean isOnline() {
        return server.isConnected();
    }

    @Override
    public GBFile[] listDirectory(GBFile father) throws ClientException {

        try {
            // I know, it's long, but i couldn't resist

            // Let me explain, it's easy
            // In this line you return a new array of GBFile. This array is created by Gson wrapping
            // the incoming json from the server. When you made this request?
            // Look at the middle of the statement, you'll find a 'server.makeQuery'. Here you make the request through ws.
            // This request return to you a FutureTask, and you block this thread calling the 'get' method.
            return gson.fromJson(((JsonObject) server.makeQuery("listFile", gson.toJsonTree(father, GBFile.class)).get()).getAsJsonArray("children"), GBFile[].class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void getFile(GBFile file) throws ClientException {
        try {
            getFile(file, new FileOutputStream(file.toFile()));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void uploadFile(GBFile file, InputStream stream) throws ClientException {
        try {

            // Get the url to upload the file
            URL url = urls.get("uploadFile", gson.toJsonTree(file), true);


            // Create a new https connection
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();


            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Authorize it
            auth.authorize(conn);


            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", String.valueOf(file.getSize()));


            // Send the file
            ByteStreams.copy(stream, conn.getOutputStream());


            int responseCode = conn.getResponseCode();


            if (responseCode != 200)
                log.warning("Response Code: " + responseCode);


            // Close the http connection
            conn.disconnect();
            stream.close();
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void uploadFile(GBFile file) throws ClientException {
        try {
            uploadFile(file, new FileInputStream(file.toFile()));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void removeFile (GBFile file) throws ClientException{
        // Make the request trough handlers socket
        try {
            server.makeQuery("removeFile", gson.toJsonTree(file), new WSQueryResponseListener() {
                @Override
                public void onResponse(JsonElement response) {
                    log.fine("File deletion response: " + response.toString());
                }
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {

    }

    @Override
    public void updateFile(GBFile file) {

    }

    /**
     * Set the listener for the event from the it.simonedegiacomi.storage.
     * Id another listener is already set, the listener
     * will be replaced
     * @param listener Listener that will called with the relative
     */
    public void setSyncEventListener(SyncEventListener listener) {

        // If the listener passed is null, remove the old listener
        // (or do nothing if was never set)
        if (listener == null)
            server.removeListener("syncEvent");

        // Add a new listener onEvent the handlers socket
        server.onEvent("syncEvent", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                // Wrap the data in a new SyncEvent
                SyncEvent event = gson.fromJson(data, SyncEvent.class);

                // Check if this is the notification for a event that i've generated.
                if(eventsToIgnore.remove(event.getRelativeFile().toFile()))
                    // Because i've generated this event, i ignore it
                    return;

                // And call the listener
                listener.on(event);
            }
        });
    }

    @Override
    public void requestEvents(long lastHeardId) {
        JsonObject request = new JsonObject();
        request.addProperty("ID", lastHeardId);
        server.sendEvent("getEventsList", request, false);
    }
}