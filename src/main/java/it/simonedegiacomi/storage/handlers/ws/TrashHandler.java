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
import it.simonedegiacomi.storage.*;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;

import java.io.IOException;
import java.util.List;

/**
 * Created on 13/04/16.
 * @author Degiacomi Simone
 */
public class TrashHandler {

    private final Gson gson = new Gson();

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

                // Prepare the response
                JsonObject response = new JsonObject();
                try {

                    // Wrap the file from the request
                    GBFile fileToTrash = gson.fromJson(jsonElement, GBFile.class);

                    // Hide the file
                    MyFileUtils.trash(fileToTrash.toFile());

                    // Update the db
                    db.trashFile(fileToTrash.getID(), true);

                    response.addProperty("success", true);
                } catch (StorageException ex) {

                    response.addProperty("success", false);
                } catch (IOException ex) {

                    response.addProperty("success", false);
                }

                return response;
            }
        };
    }

    public WSQueryHandler getRecoverHandler () {

        return new WSQueryHandler() {

            @WSQuery(name = "recoverFile")
            @Override
            public JsonElement onQuery(JsonElement jsonElement) {

                // Prepare the response
                JsonObject response = new JsonObject();
                try {

                    // Wrap the file from the request
                    GBFile fileToTrash = gson.fromJson(jsonElement, GBFile.class);

                    // Hide the file
                    MyFileUtils.untrash(fileToTrash.toFile());

                    // Update the db
                    db.trashFile(fileToTrash.getID(), false);

                    response.addProperty("success", true);
                } catch (StorageException ex) {

                    response.addProperty("success", false);
                } catch (IOException ex) {

                    response.addProperty("success", false);
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
                    response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>() {
                    }.getType()));

                    response.addProperty("error", false);
                } catch (StorageException ex) {
                    response.addProperty("error", true);
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

                // Create the response object
                JsonObject res = new JsonObject();

                // Wrap the file to delete
                GBFile fileToRemove = gson.fromJson(data, GBFile.class);
                fileToRemove.setPrefix(PATH);

                try {

                    // Get the path of the file. Maybe the wrapped file already contains
                    // the path, but maybe contains only the id
                    db.findPath(fileToRemove);

                    // Tell the internal client to ignore this event
                    watcher.startIgnoring(fileToRemove.toFile());

                    // Remove the file from the file system
                    MyFileUtils.deleteR(fileToRemove.toFile());

                    watcher.stopIgnoring(fileToRemove.toFile());

                    // And then remove the file from the database
                    SyncEvent event = db.removeFile(fileToRemove);

                    // Finally complete the response
                    res.addProperty("success", true);

                    // And send the notification will contain the deleted file
                    emitter.emitEvent(event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    res.addProperty("success", false);
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
                    List<GBFile> files = db.getTrashList(0, Long.MAX_VALUE);

                    // Iterate each file
                    for (GBFile file : files) {

                        // Set the environment prefix
                        file.setPrefix(PATH);

                        // Delete the hidden file
                        MyFileUtils.deleteR(file.toFile(), true);

                        // Remove form the database
                        db.removeFile(file.getID());
                    }

                    response.addProperty("success", true);
                } catch (StorageException ex) {
                    response.addProperty("success", false);
                }
                return response;
            }
        };
    }
}
