package it.simonedegiacomi.goboxapi.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.*;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an implementation of the gobox api client
 * interface. This client uses WebSocket to transfer
 * the file list, to authenticate and to share events
 * and use HTTP(s) to transfer the files.
 *
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class StandardClient implements Client {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(StandardClient.class.getName());

    /**
     * Configuration of the environment
     */
    private final Config config = Config.getInstance();

    /**
     * Object used to create the urls.
     */
    private final URLBuilder urls = config.getUrls();

    /**
     * WebSocket connection to the server
     */
    private MyWSClient server;

    /**
     * Authorization object used to make the call
     */
    private final Auth auth;

    /**
     * Construct a sinc object, but first try to login to gobox.
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
            server.on("open", new WSEventListener() {
                @Override
                public void onEvent(JSONObject data) {

                    log.fine("Authentication trough webSocket");

                    // Send the authentication object

                    // TODO: handle an authentication error
                    server.sendEvent("authentication", auth.toJSON(), true);
                }
            });

            // Register the storageInfo event
            server.on("storageInfo", new WSEventListener() {
                @Override
                public void onEvent(JSONObject data) {
                    System.out.println(data);
                }
            });

            // Start listening on the websocket, connecting to the server
            server.connect();

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    /**
     * Download a file from the it.simonedegiacomi.storage copying the file to the
     * dst Outputstream.
     *
     * @param file File to download.
     * @param dst Output stream where put the content of the file.
     * @throws ClientException
     */
    @Override
    public void getFile (GBFile file, OutputStream dst) throws ClientException {
        try {

            // Create and fill the request object
            JSONObject request = new JSONObject();
            request.put("ID", file.getID());

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
            FutureTask<JSONObject> future = server.makeQuery("createFolder", new JSONObject(new Gson().toJson(newDir)));
            JSONObject response = future.get();
            newDir.setID(response.getLong("newFolderId"));
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
    public List<GBFile> listDirectory(GBFile father) throws ClientException {

        // Make the quey
        FutureTask<JSONObject> future = server.makeQuery("listFile", father.toJSON());
        try {
            // Get hte response from the future
            JSONObject response = future.get();

            // Create a new array of GBFile from the response
            JSONArray children = response.getJSONArray("children");
            List<GBFile> files = new LinkedList<>();
            for (int i = 0; i < children.length(); i++)
                files.add(new GBFile(children.getJSONObject(i)));
            return files;
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        return null;
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
            URL url = urls.get("uploadFile", new JSONObject(new Gson().toJson(file)));

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
        // Make the request trough web socket
        try {
            server.makeQuery("removeFile", file.toJSON(), new WSQueryResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
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

        // Add a new listener on the web socket
        server.on("syncEvent", new WSEventListener() {
            @Override
            public void onEvent(JSONObject data) {

                // Wrap the data in a new SyncEvent
                SyncEvent event = new SyncEvent(data);

                // And call the listener
                listener.on(event);
            }
        });
    }
}