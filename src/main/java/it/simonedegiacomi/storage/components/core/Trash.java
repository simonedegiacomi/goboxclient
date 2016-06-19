package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created on 28/05/16.
 *
 * @author Degiacomi Simone
 */
public class Trash implements GBModule {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(Trash.class);

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    private Dao<GBFile, Long> fileTable;

    private Dao<SyncEvent, Long> eventTable;

    /**
     * Path of the GoBox files folder
     */
    private String PATH;

    /**
     * Event emitter
     */
    private EventEmitter eventEmitter;

    /**
     * ile system watcher
     */
    private MyFileSystemWatcher fileSystemWatcher;


    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {

        // Get the event emitter
        eventEmitter = env.getEmitter();

        // Get the used file system watcher
        fileSystemWatcher = env.getFileSystemWatcher();

        // Get the path of the files
        PATH = env.getGlobalConfig().getFolder("path", "files/").getAbsolutePath();

        // Create the trash directory
        env.getGlobalConfig().getFolder("trash", "trash/").mkdirs();
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "trashFile")
    public JsonElement onTrashQuery(JsonElement jsonElement) {

        log.info("New trash file request");

        // Prepare the response
        JsonObject json = jsonElement.getAsJsonObject();
        JsonObject response = new JsonObject();

        if (!json.has("file")) {
            log.warn("Missing request parameters");
            response.addProperty("success", false);
            response.addProperty("error", "Missing file");
            return response;
        }

        boolean toTrash = json.has("toTrash") && json.get("toTrash").getAsBoolean();

        try {

            // Wrap the file from the request
            GBFile file = DBCommonUtils.getFile(fileTable, gson.fromJson(json.get("file"), GBFile.class));

            // Check if the file exists
            if (file == null) {
                log.warn("File not found");
                response.addProperty("success", false);
                response.addProperty("error", "File not found");
                return response;
            }

            // Find the path of the file
            DBCommonUtils.findPath(fileTable, file);
            file.setPrefix(PATH);

            // Tell the fs watcher to ignore the file
            fileSystemWatcher.startIgnoring(file.toFile());


            fileSystemWatcher.foresee(file.toFile());

            file.setTrashed(toTrash);
            MyFileUtils.moveTrash(file);

            // Stop ignoring
            fileSystemWatcher.stopIgnoring(file.toFile());

            // Update the db
            fileTable.update(file);

            log.info("Request completed");

            // Register and emit the event
            SyncEvent event = new SyncEvent(toTrash ? SyncEvent.EventKind.FILE_TRASHED : SyncEvent.EventKind.FILE_RECOVERED, file);
            eventTable.create(event);
            eventEmitter.emitEvent(event);

            response.addProperty("success", true);
        } catch (SQLException ex) {
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }

    @WSQuery(name = "trashedFiles")
    public JsonElement onTrashedFilesListQuery(JsonElement jsonElement) {

        log.info("New file trash list query");

        // Prepare the response
        JsonObject response = new JsonObject();

        try {
            // Make the query
            List<GBFile> files = fileTable.queryBuilder()
                    .where()
                    .eq("trashed", true)
                    .query();

            // Add the files to the response
            response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>() {
            }.getType()));
            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }

    @WSQuery(name = "delete")
    public JsonElement onRemoveQuery(JsonElement data) {

        log.info("New remove query");

        // Create the response object
        JsonObject res = new JsonObject();

        try {

            // Wrap the file to delete
            GBFile file = DBCommonUtils.getFile(fileTable, gson.fromJson(data, GBFile.class));

            // Check if the file exists
            if (file == null) {
                log.warn("File not found");
                res.addProperty("success", false);
                res.addProperty("error", "File not found");
                return res;
            }

            // Find the path
            DBCommonUtils.findPath(fileTable, file);

            boolean alreadyTrashed = file.isTrashed();
            if (!alreadyTrashed) {

                file.setPrefix(PATH);

                // Tell the fs watcher to ignore this event
                fileSystemWatcher.startIgnoring(file.toFile());
            }

            // Remove the file from the file system
            MyFileUtils.delete(file);

            if (!alreadyTrashed) {
                fileSystemWatcher.stopIgnoring(file.toFile());
            }

            // Update the database
            fileTable.delete(file);

            // Register and emit the event
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.FILE_DELETED, file);
            eventTable.create(event);
            eventEmitter.emitEvent(event);

            // Complete the response
            res.addProperty("success", true);

            log.info("File deleted");
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            res.addProperty("success", false);
            res.addProperty("error", ex.toString());
        }
        return res;
    }

    @WSQuery(name = "emptyTrash")
    public JsonElement onQuery(JsonElement jsonElement) {

        log.info("New empty trash request");

        // Prepare the response
        JsonObject response = new JsonObject();

        try {
            // Get all the trashed files
            List<GBFile> files = fileTable.queryBuilder()
                    .where()
                    .eq("trashed", true)
                    .query();

            // Iterate each file
            for (GBFile file : files) {

                // Find the path and also the children
                DBCommonUtils.findPath(fileTable, file);
                if (file.isDirectory()) {
                    DBCommonUtils.findChildren(fileTable, file);
                }

                if (!file.isTrashed()) {

                    // Set the environment prefix
                    file.setPrefix(PATH);

                    // Ignore the event
                    fileSystemWatcher.startIgnoring(file.toFile());
                }

                // Delete the hidden file
                MyFileUtils.delete(file);

                if (!file.isTrashed()) {
                    fileSystemWatcher.stopIgnoring(file.toFile());
                }

                // Remove the file from the database
                fileTable.delete(file);

                // Register and emit the event
                SyncEvent event = new SyncEvent(SyncEvent.EventKind.FILE_DELETED, file);
                eventTable.create(event);
                eventEmitter.emitEvent(event);
            }

            log.info("Request completed");

            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }
}