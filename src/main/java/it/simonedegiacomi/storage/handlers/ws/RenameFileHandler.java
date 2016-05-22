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
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidParameterException;

/**
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class RenameFileHandler implements WSQueryHandler{

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(RenameFileHandler.class);

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Path of the GoBox files folder
     */
    private final String PATH = Config.getInstance().getProperty("path");

    /**
     * Database
     */
    private final StorageDB db;

    /**
     * Event emitter to advice all the clients
     */
    private final EventEmitter emitter;

    /**
     * File system watcher
     */
    private final MyFileSystemWatcher watcher;

    public RenameFileHandler(StorageEnvironment env) {
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
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }
}