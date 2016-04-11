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
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.FileSystemWatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Degiacomi Simone
 * Created on 14/02/16.
 */
public class CopyOrCutHandler implements WSQueryHandler {

    private final StorageDB db;

    private final FileSystemWatcher watcher;

    private final EventEmitter emitter;

    private final String PATH = Config.getInstance().getProperty("path");

    private final Gson gson = new Gson();

    public CopyOrCutHandler(StorageEnvironment env) {
        this.db = env.getDB();
        this.watcher = env.getSync().getFileSystemWatcher();
        this.emitter = env.getEmitter();
    }

    @WSQuery(name = "copyOrCutFile")
    @Override
    public JsonElement onQuery(JsonElement data) {

        // Prepare the response
        JsonObject response = new JsonObject();

        // Cast the request
        JsonObject json = data.getAsJsonObject();

        // Get the cut flag from the request
        boolean cut = json.get("cut").getAsBoolean();

        try {

            // Get the file from the database
            GBFile file = db.getFileById(json.get("id").getAsLong());

            // Get the new father from the database
            GBFile newFather = db.getFileById(json.get("fatherId").getAsLong());

            // Create the new file
            GBFile newFile = new GBFile(file.getName(), newFather.getID(), file.isDirectory());

            // Set the path of this environment
            file.setPrefix(PATH);
            newFather.setPrefix(PATH);
            newFile.setPrefix(PATH);

            while (newFile.toFile().exists()) {
                newFile.setName(newFile.getName() + " - Copy");
            }

            // Tell the client to ignore this event (the copy)
            watcher.startIgnoring(newFile);

            // Copy the file to the new destination
            copyR(file.toFile(), newFile.toFile());

            // Stop ignoring this file
            watcher.stopIgnoring(newFile);

            // Create a new Sync event
            SyncEvent creationEvent = db.copyFile(file, newFile);

            // Emit it
            emitter.emitEvent(creationEvent);

            if (cut) {

                // Tell the file system watcher to ignore
                watcher.startIgnoring(file);

                RemoveFileHandler.deleteR(file.toFile());

                watcher.stopIgnoring(file);

                SyncEvent deletion = db.removeFile(newFile);
                emitter.emitEvent(deletion);
            }

            // Complete the response with the result
            response.addProperty("success", true);
        } catch (IOException ex) {
            ex.printStackTrace();

            // Something wrong
            response.addProperty("success", false);
        } catch (StorageException ex) {

            ex.printStackTrace();

            // Something wrong
            response.addProperty("success", false);
        }
        return response;
    }

    private static void copyR (File src, File dst) throws IOException {
        Files.copy(src.toPath(), dst.toPath());
        if (src.isDirectory()) {
            for(File file : src.listFiles())
                copyR(file, new File(dst.getPath() + '/' + file.getName()));
        }
    }
}