package it.simonedegiacomi.storage.components.core;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * This component receive incoming upload from the clients
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class ClientToStorage implements GBComponent {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(ClientToStorage.class);

    /**
     * Urls
     */
    private final URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    private String PATH;

    /**
     * User credentials
     */
    private GBAuth auth;

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database event table
     */
    private Dao<SyncEvent, Long> eventTable;

    /**
     * File system watcher used in the environment
     */
    private MyFileSystemWatcher fileSystemWatcher;

    /**
     * Event emitter to notify other client and components
     */
    private EventEmitter eventEmitter;

    /**
     * Set of incoming file relative path strings.
     * TODO: Create a dedicated object to handle blocked resources
     */
    private final Set<String> incomingFilesPath = new HashSet();

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        fileSystemWatcher = env.getFileSystemWatcher();
        eventEmitter = env.getEmitter();
        auth = env.getGlobalConfig().getAuth();
        PATH = env.getGlobalConfig().getProperty("path", "files/");
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
        } catch (SQLException ex) {
            log.warn("Unable to create dao", ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "comeToGetTheFile")
    public JsonElement onBridgeUpload (JsonElement data) {

        log.info("New upload request from a bridge client");

        // Prepare the response
        JsonObject response = new JsonObject();

        // Wrap the incoming file
        GBFile incomingFile = gson.fromJson(data, GBFile.class);

        try {

            // Check if the file already exists
            GBFile old = DBCommonUtils.getFile(fileTable, incomingFile);
            boolean replace = old != null;

            if (replace) {
                log.info("Upload request is to replace a file");

                // Find the path of the old file
                DBCommonUtils.findPath(fileTable, old);
                old.setPrefix(PATH);
                incomingFile = old;
            } else {

                // Find the father
                GBFile father = DBCommonUtils.getFile(fileTable, incomingFile.getFather());

                if (father == null) {
                    log.warn("Father of the incoming file isn't known");
                    response.addProperty("success", false);
                    response.addProperty("error", "Father not found");
                    response.addProperty("httpCode", 400);
                    return response;
                }

                // Find the father path
                DBCommonUtils.findPath(fileTable, father);
                father.setPrefix(PATH);

                // Generate the child
                incomingFile = father.generateChild(incomingFile.getName(), false);
            }

            // Get the file path string
            String filePathString = incomingFile.getPathAsString();

            // Check if the file is locked
            boolean locked;
            synchronized (incomingFilesPath) {

                // Check if is locked
                locked = incomingFilesPath.contains(filePathString);

                // If it's not locked, locked it now
                if (!locked) {
                    incomingFilesPath.add(filePathString);
                }
            }

            if (locked) {
                log.warn("Upload failed because the file is locked by another upload");
                response.addProperty("success", false);
                response.addProperty("message", "File already in use");
                response.addProperty("httpCode", 500);
                return response;
            }

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
            jsonReq.addProperty("uploadKey", data.getAsJsonObject().get("uploadKey").getAsString());

            // Send this json
            conn.getOutputStream().write(jsonReq.toString().getBytes());

            // Attend for the response ...
            if(conn.getResponseCode() != 200) {
                log.warn("Upload file from client to storage failed: " + conn.getResponseMessage());
                conn.disconnect();
                response.addProperty("success", false);
                response.addProperty("error", "http request failed");
                response.addProperty("httpCode", 500);
                return response;
            }

            // Tell the internal client to ignore the creation of the file
            fileSystemWatcher.startIgnoring(incomingFile.toFile());

            // Create the stream to the disk
            DataOutputStream toDisk = new DataOutputStream(new FileOutputStream(incomingFile.toFile()));

            // Copy the stream
            ByteStreams.copy(conn.getInputStream(), toDisk);

            // Close file and http
            toDisk.close();
            conn.disconnect();

            // Read the info of the file
            MyFileUtils.loadFileAttributes(incomingFile);

            // Update the database
            SyncEvent event;
            if (replace) {

                // Update the old file
                fileTable.update(incomingFile);
                event = new SyncEvent(SyncEvent.EventKind.FILE_MODIFIED, incomingFile);
            } else {

                // Insert the new file
                fileTable.create(incomingFile);
                event = new SyncEvent(SyncEvent.EventKind.FILE_CREATED, incomingFile);
            }

            // The notification will contain the new file information
            eventEmitter.emitEvent(event);
            eventTable.create(event);

            // Unlock the file
            synchronized (incomingFilesPath) {
                incomingFilesPath.remove(filePathString);
            }

            // Stop ignoring the new file
            fileSystemWatcher.stopIgnoring(incomingFile.toFile());

            // Successful request!
            response.addProperty("success", true);
        } catch (SQLException ex) {
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            response.addProperty("httpCode", 500);
        } catch (IOException ex) {
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            response.addProperty("httpCode", 500);
        }

        return response;
    }
}