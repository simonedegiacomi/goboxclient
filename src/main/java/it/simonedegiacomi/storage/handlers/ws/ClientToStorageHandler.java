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
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidParameterException;

/**
 * This handler is used to send a file from the storage
 * to the client that requested it.
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 */
public class ClientToStorageHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(ClientToStorageHandler.class.getName());

    /**
     * Gson to serialize/deserialize
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Configuration
     */
    private final Config config = Config.getInstance();

    /**
     * Account to use with this storage
     */
    private final Auth auth = config.getAuth();

    /**
     * Urls to use
     */
    private final URLBuilder urls = config.getUrls();

    /**
     * GoBox files folder
     */
    private final String PATH = config.getProperty("path");

    /**
     * Sync event emitter that broadcast event to all the clients
     */
    private final EventEmitter emitter;

    /**
     * File system watcher for the GoBox files ({@link #PATH} folder
     */
    private final FileSystemWatcher watcher;

    /**
     * Database to store files information, sharing and events
     */
    private final StorageDB db;

    /**
     * Create a new Client to storage handler. The environment must have a valid File system sync, a valid db and a valid
     * event emitter
     * @param env Environment in which is the handler run
     */
    public ClientToStorageHandler(StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("Environment without db");

        if (env.getEmitter() == null)
            throw new InvalidParameterException("Environment without event emitter");

        if (env.getSync() == null || env.getSync().getFileSystemWatcher() == null)
            throw new InvalidParameterException("Environment without file system watcher");

        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

    @WSQuery(name = "comeToGetTheFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("New come to get upload request");
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject queryResponse = new JsonObject();

        // Assert that the name of the file is present and also the father and the upload key
        if (!json.has("uploadKey") || !json.has("name") || !json.has("father")) {
            queryResponse.addProperty("success", false);
            queryResponse.addProperty("error", "missing data");
            queryResponse.addProperty("httpCode", 400);
            return queryResponse;
        }

        // Get the uploadKey
        String uploadKey = json.get("uploadKey").getAsString();

        // Wrap the incoming file and the father
        GBFile father = gson.fromJson(json.get("father"), GBFile.class);

        try {

            // Get information about the father
            GBFile dbFather = db.getFile(father, true, true);

            if (dbFather == null) {
                queryResponse.addProperty("success", false);
                queryResponse.addProperty("error", "father not found");
                queryResponse.addProperty("httpCode", 400);
                return queryResponse;
            }

            // Set the environment prefix
            dbFather.setPrefix(PATH);

            // Create the new child
            GBFile child = dbFather.generateChild(json.get("name").getAsString(), false);

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
            JsonObject jsonReq = new JsonObject();
            jsonReq.addProperty("uploadKey", uploadKey);

            // Send this json
            conn.getOutputStream().write(jsonReq.toString().getBytes());

            // Attend for the response ...
            if(conn.getResponseCode() != 200) {
                log.warn("Upload file from client to storage failed");
                queryResponse.addProperty("success", false);
                queryResponse.addProperty("error", "http request failed");
                queryResponse.addProperty("httpCode", 500);
                return queryResponse;
            }

            // Tell the internal client to ignore the creation of the file
            watcher.startIgnoring(child.toFile());

            // Create the stream to the disk
            DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(child.toFile()));

            // Copy the stream
            ByteStreams.copy(conn.getInputStream(), toDisk);

            // Close file and http
            toDisk.close();
            conn.disconnect();

            // Read the info of the file
            MyFileUtils.loadFileAttributes(child);

            // Insert the file in the database
            SyncEvent event = db.insertFile(child);

            // The notification will contain the new file information
            emitter.emitEvent(event);

            // Stop ignoring the new file
            watcher.stopIgnoring(child.toFile());

            // Successful request!
            queryResponse.addProperty("success", true);
        } catch (IOException ex) {

            log.warn(ex.toString(), ex);
            queryResponse.addProperty("success", false);
            queryResponse.addProperty("error", "IOError");
            queryResponse.addProperty("httpCode", 500);
        } catch (StorageException ex) {

            log.warn(ex.toString(), ex);
            queryResponse.addProperty("success", false);
            queryResponse.addProperty("error", ex.toString());
            queryResponse.addProperty("httpCode", 500);
        }
        return queryResponse;
    }
}