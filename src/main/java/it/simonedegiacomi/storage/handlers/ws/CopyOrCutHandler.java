package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.*;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;

import java.io.IOException;

/**
 * @author Degiacomi Simone
 * Created on 14/02/16.
 */
public class CopyOrCutHandler implements WSQueryHandler {

    private final StorageDB db;

    private final FileSystemWatcher watcher;

    private final EventEmitter emitter;

    private final String PATH = Config.getInstance().getProperty("path");

    private final Gson gson = MyGsonBuilder.create();

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
        boolean cut = json.has("cut") ? json.get("cut").getAsBoolean() : false;

        if (!json.has("file") || !json.has("newFather")) {

            response.addProperty("success", false);
            response.addProperty("error", "missing file");
            return response;
        }

        // Instance the file from the json
        GBFile file = gson.fromJson(json.get("file"), GBFile.class);

        // Instance the new father from the json
        GBFile newFather = gson.fromJson(json.get("newFather"), GBFile.class);

        GBFile dbFile, dbFather;

        try {

            // I need to place another try/catch, so i can use the finally block in the second try/catch
            // to stop ignoring the file.

            // Get the file with all the information
            dbFile = db.getFile(file);
            dbFather = db.getFile(newFather);

            if (file == null || dbFather == null) {
                response.addProperty("success", false);
                response.addProperty("error", "file not found");
                return response;
            }

            dbFather.setPrefix(PATH);
            dbFather.setPrefix(PATH);
        } catch (StorageException ex) {
            response.addProperty("success", false);
            return response;
        }

        try {

            // Generate the new file
            GBFile newFile = dbFather.generateChild(dbFile.getName(), dbFile.isDirectory());

            // Assert that the name is unique
            while (newFile.toFile().exists()) {
                newFile.setName(newFile.getName() + " - Copy");
            }

            // Tell the client to ignore this event (the copy)
            watcher.startIgnoring(newFile.toFile());
            watcher.startIgnoring(dbFile.toFile());

            // Copy the file to the new destination
            MyFileUtils.copyR(dbFile.toFile(), newFile.toFile());

            // Create a new Sync event
            SyncEvent creationEvent = db.copyFile(dbFile.getID(), dbFather.getID(), newFile.getName());

            // Emit it
            emitter.emitEvent(creationEvent);

            // Remove the files if the operation is a cut
            if (cut) {

                // Delete from the disk
                MyFileUtils.deleteR(dbFile.toFile());

                SyncEvent deletion = db.removeFile(dbFile.getID());
                emitter.emitEvent(deletion);
            }

            // Stop ignoring this file
            watcher.stopIgnoring(newFile.toFile());
        } catch (IOException ex) {

            response.addProperty("success", false);
            response.addProperty("error", "IO error");
            return response;
        } catch (StorageException ex) {

            // TODO: If the event is not insert in the database, i should remove the file copy
            response.addProperty("success", false);
            return response;
        } finally {
            watcher.stopIgnoring(dbFile.toFile());
        }

        // Complete the response
        response.addProperty("success", true);
        return response;
    }
}