package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * This handler manage all the ws query to manage the trash and to delete files
 * Created on 13/04/16.
 * @author Degiacomi Simone
 */
public class TrashHandler {

    private final static Logger log = Logger.getLogger(TrashHandler.class);

    private final Gson gson = MyGsonBuilder.create();

    private final String PATH = Config.getInstance().getProperty("path");

    private final EventEmitter emitter;

    private final FileSystemWatcher watcher;

    private final StorageDB db;

    public TrashHandler (StorageEnvironment env) {
        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

    /**
     * Return the handler that is called to trash a file
     * @return Handler that trash files
     */
    public WSQueryHandler getTrashHandler () {
        return new WSQueryHandler() {

            @WSQuery(name = "trashFile")
            @Override
            public JsonElement onQuery(JsonElement jsonElement) {
                JsonObject json = jsonElement.getAsJsonObject();

                // Prepare the response
                JsonObject response = new JsonObject();
                if (!json.has("toTrash") || !json.has("file")) {
                    response.addProperty("success", false);
                    response.addProperty("error", "missing file and/or trash flag");
                    return response;
                }

                boolean toTrash = json.get("toTrash").getAsBoolean();

                try {

                    // Wrap the file from the request
                    GBFile file = gson.fromJson(json.get("file"), GBFile.class);
                    GBFile dbFile = db.getFile(file, true, false);
                    dbFile.setPrefix(PATH);

                    // Assert that the file exist
                    if (dbFile == null) {
                        response.addProperty("success", false);
                        response.addProperty("error", "file not found");
                        return response;
                    }

                    // Hide the file
                    watcher.startIgnoring(dbFile.toFile());
                    dbFile.setTrashed(toTrash);
                    MyFileUtils.moveTrash(dbFile);
                    watcher.stopIgnoring(dbFile.toFile());

                    // Update the db
                    db.trashFile(dbFile, toTrash);

                    response.addProperty("success", true);
                } catch (StorageException ex) {
                    response.addProperty("success", false);
                } catch (IOException ex) {
                    response.addProperty("success", false);
                    response.addProperty("error", ex.toString());
                }

                return response;
            }
        };
    }

    /**
     * Return the handler that sends the list of the trashed files
     * @return Handler that sends the list of the trashed files
     */
    public WSQueryHandler getTrashedHandler () {
        return new WSQueryHandler() {

            @WSQuery(name = "trashedFiles")
            @Override
            public JsonElement onQuery(JsonElement jsonElement) {

                // Prepare the response
                JsonObject response = new JsonObject();

                try {
                    // Make the query
                    List<GBFile> files = db.getTrashList();

                    // Add the files to the response
                    response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>() {}.getType()));
                    response.addProperty("success", true);
                } catch (StorageException ex) {
                    log.warn(ex.toString(), ex);
                    response.addProperty("success", false);
                    response.addProperty("error", ex.toString());
                }
                return response;
            }
        };
    }

    /**
     * Return the handler that delete the files
     * @return Handler that delete the files
     */
    public WSQueryHandler getDeleteHandler () {
        return new WSQueryHandler() {
            @WSQuery(name = "removeFile")
            @Override
            public JsonElement onQuery (JsonElement data) {
                JsonObject json = data.getAsJsonObject();

                // Create the response object
                JsonObject res = new JsonObject();

                try {

                    // Wrap the file to delete
                    GBFile file = gson.fromJson(data, GBFile.class);
                    GBFile dbFile = db.getFile(file, true, true);

                    // Tell the internal client to ignore this event
                    watcher.startIgnoring(dbFile.toFile());

                    // Remove the file from the file system
                    MyFileUtils.delete(dbFile);

                    watcher.stopIgnoring(dbFile.toFile());

                    // And then remove the file from the database
                    SyncEvent event = db.removeFile(dbFile);

                    // Finally complete the response
                    res.addProperty("success", true);

                    // And send the notification will contain the deleted file
                    emitter.emitEvent(event);
                } catch (StorageException ex) {
                    log.warn(ex.toString(), ex);
                    res.addProperty("success", false);
                    res.addProperty("error", ex.toString());
                }
                return res;
            }
        };
    }

    /**
     * Handler that empty the trash deleting all the trashed files
     * @return The new handler
     */
    public WSQueryHandler getEmptyTrashHandler () {
        return new WSQueryHandler() {

            @WSQuery(name = "emptyTrash")
            @Override
            public JsonElement onQuery(JsonElement jsonElement) {

                // Prepare the response
                JsonObject response = new JsonObject();

                try {
                    // Get all the trashed files
                    List<GBFile> files = db.getTrashList();

                    // Iterate each file
                    for (GBFile file : files) {

                        // Find the path and also the children
                        db.findPath(file);
                        db.findChildren(file);

                        // Set the environment prefix
                        file.setPrefix(PATH);

                        // Delete the hidden file
                        MyFileUtils.delete(file);

                        // Remove form the database
                        db.removeFile(file);
                    }

                    response.addProperty("success", true);
                } catch (StorageException ex) {
                    response.addProperty("success", false);
                    response.addProperty("error", ex.toString());
                }
                return response;
            }
        };
    }
}