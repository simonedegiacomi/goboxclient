package goboxapi.client;

import goboxapi.authentication.Auth;
import goboxapi.GBFile;
import goboxapi.MyWS.MyWSClient;
import goboxapi.MyWS.WSEvent;
import goboxapi.MyWS.WSQueryResponseListener;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class StandardClient implements Client {
    private final Auth auth;
    private MyWSClient server;
    private ClientEventListener eventListener;

    public StandardClient(Auth auth) {
        this.auth = auth;
        // Connect tothe server
        try {
            server = new MyWSClient(new URI("ws://goboxserver-simonedegiacomi.c9users.io/api/ws/client"));
            server.on("open", new WSEvent() {
                @Override
                public void onEvent(JSONObject data) {
                    // Send the authentication object
                    System.out.println(auth.toJSON());
                    server.sendEvent("authentication", auth.toJSON(), true);
                }
            });
            server.on("storageInfo", new WSEvent() {
                @Override
                public void onEvent(JSONObject data) {
                    System.out.println(data);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assingSocketEventListener();
    }

    @Override
    public InputStream getFile (GBFile file) {
        try {
            URL url = new URL("");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        } catch (Exception ex) {

        }
        return null;
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
                // The request is a multi part, with a field
                // for each property and one field for the file
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeFile (GBFile file) {
        // Make the requestt trought websockets
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
    public void updateFile(GBFile file) {

    }

    @Override
    public void assignEventListener (ClientEventListener listener) {
        this.eventListener = listener;
    }

    private void assingSocketEventListener() {
        server.on("newFile", new WSEvent() {
            @Override
            public void onEvent(JSONObject data) {
                // wrap the data into a SyncEvent
                // Call the listener
            }
        });
    }
}
