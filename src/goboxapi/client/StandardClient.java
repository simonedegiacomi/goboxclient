package goboxapi.client;

import com.google.common.io.ByteStreams;
import configuration.Config;
import goboxapi.GBFile;
import goboxapi.MyWS.MyWSClient;
import goboxapi.MyWS.WSEvent;
import goboxapi.MyWS.WSQueryResponseListener;
import goboxapi.URLBuilder;
import goboxapi.authentication.Auth;
import goboxapi.utils.URLParams;
import org.json.JSONObject;
com.google.common.io.ByteStreams
import sun.misc.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an implementation of the gobox api client
 * onterface. This client uses WebSocket to transfer
 * the file list, to authenticate and to share events
 * annd use HTTP to trasfer the files
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class StandardClient implements Client {

    private static final Logger log = Logger.getLogger(StandardClient.class.getName());

    private final Config config = Config.getInstance();

    private final URLBuilder urls = config.getUrls();

    /**
     * WebSocket connection to the server
     */
    private MyWSClient server;

    public StandardClient(Auth auth) {

        // Load the url
        urls = Config.getInstance().getUrls();

        // Connect to the server
        try {
            // Connect to the server trough webSocket
            server = new MyWSClient(urls.get("socketStorage").toURI());
            // When the webSocket in opened, send the authentication object
            server.on("open", new WSEvent() {
                @Override
                public void onEvent(JSONObject data) {

                    log.fine("Authentication trough webSocket")

                    // Send the authentication object
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
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    /**
     * Download a file from the storage copying the file to the
     * dst Outputstream.
     * Note that if the file is a folder, the file will be a zip archive
     * @param file File to download.
     * @param dst Output stream where put the content of the file.
     */
    @Override
    public void getFile (GBFile file, OutputStream dst) {
        try {
            JSONObject request = new JSONObject();
            request.put("id", file.getID());
            URL url = URLParams.createURL(urls.get("getFile"), request);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            ByteStreams.copy(conn.getOutputStream(), dst);
            conn.disconnect();
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void getFile(GBFile file) {
        return null;
    }

    @Override
    public void uploadFile(GBFile file, Inputstream stream) {

    }

    @Override
    public void uploadFile (GBFile file) {
        try {
            if(file.isDirectory()) {
                System.out.println("file.toJSON() " + file.toJSON());
                server.makeQuery("createFolder", file.toJSON(), new WSQueryResponseListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            file.setID(response.getLong("newFolderId"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } else {
                // Make a request to upload the file
                // The information of the file are inserted in the url


                HttpsURLConnection conn = (HttpsURLConnection) URLParams.createURL("https://goboxserver-simonedegiacomi.c9users.io/api/transfer/toStorage",
                        file.toJSON()).openConnection();

                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Length", String.valueOf(file.getSize()));
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                // open the file
                DataInputStream in = new DataInputStream(new FileInputStream(file.toFile()));
                byte[] buffer = new byte[1024];
                int read = 0;
                while((read = in.read(buffer)) > 0)
                    out.write(buffer, 0, read);
                in.close();
                out.close();
                int responseCode = conn.getResponseCode();
                conn.disconnect();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeFile (GBFile file) {
        // Make the request trough websockets
        try {
            server.makeQuery("removeFile", file.toJSON(), new WSQueryResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println(response);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
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
