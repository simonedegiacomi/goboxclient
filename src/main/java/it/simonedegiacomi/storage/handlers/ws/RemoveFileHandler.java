package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @author Degiacomi Simone
 * Created on 8/2/2016
 */
public class RemoveFileHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(RemoveFileHandler.class.getName());

    private final Gson gson = new Gson();

    private final StorageDB db;

    private final String PATH = Config.getInstance().getProperty("path");

    private final EventEmitter emitter;

    private final FileSystemWatcher watcher;

    public RemoveFileHandler (StorageEnvironment env) {
        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

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
            watcher.startIgnoring(fileToRemove);

            // Remove the file from the file system
            deleteR(fileToRemove.toFile());

            watcher.stopIgnoring(fileToRemove);

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

    public static void deleteR (File fileToRemove) {
        if(fileToRemove.isDirectory()) {
            for (File file : fileToRemove.listFiles())
                deleteR(file);
        }

        // Even if it's a folder delete it, because only now is empty
        fileToRemove.delete();
    }
}
