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
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.Sync;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * This handle the request to create new directories
 *
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class CreateFolderHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(CreateFolderHandler.class.getName());

    private final Gson gson = new Gson();

    /**
     * GoBox files folder
     */
    private final String PATH = Config.getInstance().getProperty("path");

    /**
     * Storage database
     */
    private final StorageDB db;

    /**
     * Event emitter to advise the clients
     */
    private final EventEmitter emitter;

    /**
     * File system watcher of the GoBox files folder
     */
    private final Sync sync;

    public CreateFolderHandler (StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        if (env.getEmitter() == null)
            throw new InvalidParameterException("environment without event emitter");

        if (env.getSync() == null)
            throw new InvalidParameterException("environment without sync");

        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.sync = env.getSync();
    }

    @WSQuery(name = "createFolder")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("CreateFolder query");
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        // Wrap the new directory
        GBFile newFolder = gson.fromJson(json, GBFile.class);
        newFolder.setPrefix(PATH);

        try {

            // Insert the file and get the event
            SyncEvent event = db.insertFile(newFolder);

            // Tell the internal client ot ignore this event
            sync.getFileSystemWatcher().startIgnoring(newFolder.toFile());

            // Create the real file in the FS
            Files.createDirectory(newFolder.toFile().toPath());

            // Stop ignoring
            sync.getFileSystemWatcher().stopIgnoring(newFolder.toFile());

            // Then complete the response
            response.addProperty("newFolderId", newFolder.getID());
            response.addProperty("created", true);
            response.addProperty("success", true);

            // But first, send a broadcast message to advise the other
            // client that a new folder is created

            // The notification will contain the new file information
            emitter.emitEvent(event);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("created", false);
            response.addProperty("error", ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("created", false);
            response.addProperty("error", ex.toString());
        }

        // Finally return the response
        return response;
    }

    /**
     * Assert that the specified file has an unique name.
     * @param file File to check
     * @param brothers Brother of the file
     */
    private static void uniqueName (GBFile file, List<GBFile> brothers) {
        for (GBFile brother : brothers) {
            if (file.getName().equals(brother.getName())) {
                file.setName(file.getName() + " - Copy");
                uniqueName(file, brothers);
                return;
            }
        }
    }
}