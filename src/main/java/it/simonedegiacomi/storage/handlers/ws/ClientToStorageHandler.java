package it.simonedegiacomi.storage.handlers.ws;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.utils.FileInfo;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URL;

/**
 * This handler is used to send a file from the storage
 * to the client that requested it.
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 */
public class ClientToStorageHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(ClientToStorageHandler.class.getName());

    private final Config config = Config.getInstance();

    private final Auth auth = config.getAuth();

    private URLBuilder urls = config.getUrls();

    private final String PATH = config.getProperty("path");

    private final Gson gson = new Gson();

    private final EventEmitter emitter;

    private final FileSystemWatcher watcher;

    private final StorageDB db;

    public ClientToStorageHandler(StorageEnvironment env) {
        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

    @WSQuery(name = "comeToGetTheFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("New come to get upload request");

        // Prepare the response
        JsonObject queryResponse = new JsonObject();

        // Get the uploadKey
        String uploadKey = ((JsonObject) data).get("uploadKey").getAsString();

        // Wrap the incoming file
        GBFile incomingFile = gson.fromJson(data.toString(), GBFile.class);

        // Set the path of the environment
        incomingFile.setPrefix(PATH);

        try {

            // Make the https request to the main server
            URL url = urls.get("receiveFile");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Prepare the connection
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Authorize it
            auth.authorize(conn);

            // Create the json that will identify the upload
            JsonObject json = new JsonObject();
            json.addProperty("uploadKey", uploadKey);

            // Send this json

            // Specify the length of the json
            conn.setRequestProperty("Content-Length", String.valueOf(json.toString().length()));
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.println(json.toString());

            // Flush the request
            out.close();

            // Attend for the response ...
            int response = conn.getResponseCode();

            if(response != 200) {

                log.warn("Upload file from client to storage failed");
                queryResponse.addProperty("success", false);
                return queryResponse;
            }

            // Tell the internal client to ignore this event
            watcher.startIgnoring(incomingFile);

            // Find the right path
            db.findPath(incomingFile);

            // Create the stream to the disk
            DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(incomingFile.toFile()));

            // Copy the stream
            ByteStreams.copy(conn.getInputStream(), toDisk);

            // Close file
            toDisk.close();

            // And the http connection
            conn.disconnect();

            // Read the info of the file
            FileInfo.loadFileAttributes(incomingFile);

            // Insert the file in the database
            SyncEvent event = db.insertFile(incomingFile);

            // The notification will contain the new file information
            emitter.emitEvent(event);

            // Stop ignoring
            watcher.stopIgnoring(incomingFile);

            queryResponse.addProperty("success", true);

        } catch (Exception ex) {

            log.warn(ex.toString(), ex);
            queryResponse.addProperty("success", false);
            queryResponse.addProperty("error", "IOError");
            queryResponse.addProperty("httpCode", 500);
        }

        return queryResponse;
    }
}