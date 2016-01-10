package goboxapi.client;

import com.google.common.io.ByteStreams;
import configuration.Config;
import goboxapi.GBFile;
import goboxapi.MyWS.MyWSClient;
import goboxapi.MyWS.WSEvent;
import goboxapi.MyWS.WSQueryResponseListener;
import goboxapi.URLBuilder;
import goboxapi.authentication.Auth;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
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
            server = new MyWSClient(urls.get("socketStorage").toURI());

            // When the webSocket in opened, send the authentication object
            server.on("open", new WSEvent() {
                @Override
                public void onEvent(JSONObject data) {

                    log.fine("Authentication trough webSocket")

                    // Send the authentication object

                    // TODO: handle an authentication error
                    server.sendEvent("authentication", auth.toJSON(), true);
                }
            });

            // Register the storageInfo event
            server.on("storageInfo", new WSEvent() {
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
     * Download a file from the storage copying the file to the
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
            request.put("id", file.getID());

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
            URL url = urls.get("uploadFile", file.toJSON());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            auth.authorize(conn);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", String.valueOf(file.getSize()));

            // Send the file
            ByteStreams.copy(stream, conn.getOutputStream());

            conn.disconnect();
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
        // Make the request trough websockets
        try {
            server.makeQuery("removeFile", file.toJSON(), new WSQueryResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println(response);
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

    public void setSyncEventListener(SyncEventListener listener) {
        server.on("syncEvent", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                SyncEvent event = new SyncEvent(data);
                listener.on(event);
            }
        });
    }
}
