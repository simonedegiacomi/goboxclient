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
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class RenameFileHandler implements WSQueryHandler{

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(RenameFileHandler.class);

    private final String PATH = Config.getInstance().getProperty("path");

    private final StorageDB db;

    private final EventEmitter emitter;

    private final Gson gson = MyGsonBuilder.create();

    private final FileSystemWatcher watcher;

    public RenameFileHandler(StorageEnvironment env) {
        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

    @WSQuery(name = "rename")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject request = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!request.has("file") || !request.has("newName")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing data");
            return response;
        }

        // Get the file from the request
        GBFile file = gson.fromJson(request.get("file"), GBFile.class);

        // Get the new name
        String newName = request.get("newName").getAsString();

        try {

            // Fill with the info
            GBFile dbFile = db.getFile(file, true, false);
            dbFile.setPrefix(PATH);

            // Change the name on the filesystem
            File oldFile = dbFile.toFile();
            dbFile.setName(newName);
            File newFile = dbFile.toFile();

            watcher.startIgnoring(oldFile);
            watcher.startIgnoring(newFile);

            Files.move(oldFile.toPath(), newFile.toPath());

            watcher.stopIgnoring(oldFile);
            watcher.stopIgnoring(newFile);

            // Change the name in the database
            SyncEvent update = db.updateFile(dbFile);
            emitter.emitEvent(update);

            // complete the response
            response.addProperty("success", true);
        } catch (StorageException ex) {
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            log.warn(ex.toString(), ex);
        } catch (IOException ex) {
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            log.warn(ex.toString(), ex);
        }
        return response;
    }
}