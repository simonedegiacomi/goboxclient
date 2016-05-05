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
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidParameterException;

/**
 * @author Degiacomi Simone
 * Created on 14/02/16.
 */
public class CopyOrCutHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(CopyOrCutHandler.class);

    /**
     * Path of the GoBox files folder
     */
    private final String PATH = Config.getInstance().getProperty("path");

    /**
     * Gson used to serialize/deserialize
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Database to manage files
     */
    private final StorageDB db;

    /**
     * File system watcher of the GoBox files folder
     */
    private final FileSystemWatcher watcher;

    /**
     * Event emitter to notify the clients
     */
    private final EventEmitter emitter;

    /**
     * Create a new copy or cut handler with the specified environment
     * @param env Environment to use
     */
    public CopyOrCutHandler(StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        if (env.getEmitter() == null)
            throw new InvalidParameterException("environment without event emitter");

        if (env.getSync() == null || env.getSync().getFileSystemWatcher() == null)
            throw new InvalidParameterException("environment without file system watcher");

        this.db = env.getDB();
        this.watcher = env.getSync().getFileSystemWatcher();
        this.emitter = env.getEmitter();
    }

    @WSQuery(name = "copyOrCutFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!json.has("file") || !json.has("newFather")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing file");
            return response;
        }

        // Get the cut flag from the request
        boolean cut = json.has("cut") ? json.get("cut").getAsBoolean() : false;

        // Instance the file from the json
        GBFile file = gson.fromJson(json.get("file"), GBFile.class);

        // Instance the new father from the json
        GBFile newFather = gson.fromJson(json.get("newFather"), GBFile.class);

        try {

            // Get the file with all the information
            GBFile dbFile = db.getFile(file, true, true);
            GBFile dbFather = db.getFile(newFather, true, true);

            if (file == null || dbFather == null) {
                response.addProperty("success", false);
                response.addProperty("error", "file not found");
                return response;
            }

            dbFile.setPrefix(PATH);
            dbFather.setPrefix(PATH);

            // Generate the new file
            GBFile newFile = dbFather.generateChild(dbFile.getName(), dbFile.isDirectory());

            // Assert that the name is unique
            while (newFile.toFile().exists()) {
                newFile.setName(newFile.getName() + " - Copy");
            }

            // Tell the client to ignore this event (the copy)
            watcher.startIgnoring(newFile.toFile());
            watcher.startIgnoring(dbFile.toFile());

            if (cut) {

                // Just move the file
                Files.move(dbFile.toFile().toPath(), newFile.toFile().toPath());
            } else {

                // Copy the file to the new destination
                MyFileUtils.copyR(dbFile.toFile(), newFile.toFile());
            }

            // Create a new Sync event
            SyncEvent creationEvent = db.copyOrCutFile(dbFile, newFile, cut);

            // Emit it
            emitter.emitEvent(creationEvent);

            // Stop ignoring this file
            watcher.stopIgnoring(newFile.toFile());
            watcher.stopIgnoring(dbFile.toFile());

            // Complete the response
            response.addProperty("success", true);
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", "IO error");
            return response;
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            return response;
        }

        return response;
    }
}