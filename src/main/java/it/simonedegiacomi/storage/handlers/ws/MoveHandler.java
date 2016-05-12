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
public class MoveHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(MoveHandler.class);

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
    public MoveHandler(StorageEnvironment env) {
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

    @WSQuery(name = "move")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!json.has("src") || !json.has("dst") || !json.has("dstFather")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing file");
            return response;
        }

        boolean copy = json.has("copy") ? json.get("copy").getAsBoolean() : false;

        // Instance the file from the json
        GBFile src = gson.fromJson(json.get("src"), GBFile.class);

        // Instance the new father from the json
        GBFile dstFather = gson.fromJson(json.get("dstFather"), GBFile.class);

        GBFile dst = gson.fromJson(json.get("dst"), GBFile.class);

        try {

            // Get the file with all the information
            GBFile dbSrc = db.getFile(src, true, true);
            GBFile dbDstFather = db.getFile(dstFather, true, true);

            if (dbSrc == null || dbDstFather == null) {
                response.addProperty("success", false);
                response.addProperty("error", "file not found");
                return response;
            }

            dbSrc.setPrefix(PATH);
            dbDstFather.setPrefix(PATH);

            // Generate the new file
            GBFile dbDst = dbDstFather.generateChild(dst.getName(), dbSrc.isDirectory());

            // Assert that the name is unique
            while (dbDst.toFile().exists()) {
                dbDst.setName(dbDst.getName() + " - Copy");
            }

            // Tell the client to ignore this event (the copy)
            watcher.startIgnoring(dbDst.toFile());
            watcher.startIgnoring(dbSrc.toFile());

            if (copy) {

                // Copy the file to the new destination
                MyFileUtils.copyR(dbFile.toFile(), newFile.toFile());
            } else {

                // Just move the file
                Files.move(dbFile.toFile().toPath(), newFile.toFile().toPath());
            }

            // Create a new Sync event
            SyncEvent creationEvent = db.m(dbFile, newFile, cut);

            // Emit it
            emitter.emitEvent(creationEvent);

            // Stop ignoring this file
            watcher.stopIgnoring(dbDst.toFile());
            watcher.stopIgnoring(dbSrc.toFile());

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