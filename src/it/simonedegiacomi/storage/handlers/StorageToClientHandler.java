package it.simonedegiacomi.storage.handlers;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.annotations.WSEvent;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This handler is used to send a file on the storage
 * to the client that requested it
 *
 * Created by Degiacomi Simone on 07/02/16.
 */
public class StorageToClientHandler implements WSEventListener {

    private static final Logger log = Logger.getLogger(StorageToClientHandler.class.getName());

    private final Config config = Config.getInstance();

    private final Auth auth = config.getAuth();

    private URLBuilder urls = config.getUrls();

    private final String PATH = config.getProperty("path");

    private final Gson gson = new Gson();

    private final EventEmitter emitter;

    private final StorageDB db;

    public StorageToClientHandler (StorageDB db, EventEmitter emitter) {
        this.db = db;
        this.emitter = emitter;
    }

    @WSEvent(name = "comeToGetTheFile")
    @Override
    public void onEvent(JsonElement data) {
        log.info("New come to get upload request");
        try {

            // Get the uploadKey
            String uploadKey = ((JsonObject) data).get("uploadKey").getAsString();

            // Wrap the incoming file
            GBFile incomingFile = gson.fromJson(data.toString(), GBFile.class);

            // Make the https request to the main server
            URL url = urls.get("receiveFile");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            auth.authorize(conn);

            // Enable input and output fro this request
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Create the json that will identify the upload
            JsonObject json = new JsonObject();
            json.addProperty("uploadKey", uploadKey);

            // Send this json
            // specify the length of the json
            conn.setRequestProperty("Content-Length", String.valueOf(json.toString().length()));
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.println(json.toString());
            // Close the output stream of the request
            out.close();

            // get the file
            int response = conn.getResponseCode();

            // Create the stream to the disk
            DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(incomingFile.getPathAsString(PATH)));

            // Copy the stream
            ByteStreams.copy(conn.getInputStream(), toDisk);

            // Close file and http connection
            toDisk.close();
            conn.disconnect();

            // Insert the file in the database
            SyncEvent event = db.insertFile(incomingFile);

            // The notification will contain the new file information
            emitter.emitEvent(event);

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }
}