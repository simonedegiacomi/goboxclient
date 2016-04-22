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
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.FileSystemWatcher;
import org.apache.log4j.Logger;

import java.nio.file.Files;

/**
 * This handle the request to create new directories
 *
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class CreateFolderHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(CreateFolderHandler.class.getName());

    private final StorageDB db;

    private final EventEmitter emitter;

    private final FileSystemWatcher watcher;

    private final String PATH = Config.getInstance().getProperty("path");

    private final Gson gson = new Gson();

    public CreateFolderHandler (StorageEnvironment env) {

        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.watcher = env.getSync().getFileSystemWatcher();
    }

    @WSQuery(name = "createFolder")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("CreateFolder query");
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!json.has("father") || !json.has("name")) {
            response.addProperty("created", false);
            response.addProperty("error", "missing father or name");
        }

        // Wrap the new directory
        GBFile father = gson.fromJson(json.get("father"), GBFile.class);

        try {
            GBFile dbFather = db.getFile(father);

            // Check that the father exists
            if (dbFather == null) {
                response.addProperty("created", false);
                response.addProperty("errror", "father doesn't exist");
                return response;
            }

            dbFather.setPrefix(PATH);
            GBFile newFolder = dbFather.generateChild(json.get("name").getAsString(), true);

            // Check if another file with the same name already exists
            if(Files.exists(newFolder.toFile().toPath())) {
                response.addProperty("created", false);
                response.addProperty("error", "a folder with this name already exist");
                return response;
            }

            // Tell the internal client ot ignore this event
            watcher.startIgnoring(newFolder.toFile());

            // Create the real file in the FS
            Files.createDirectory(newFolder.toFile().toPath());

            // Stop ignoring
            watcher.stopIgnoring(newFolder.toFile());

            // Load the date from the fs to the logical file
            MyFileUtils.loadFileAttributes(newFolder);

            // Insert the file and get the event
            SyncEvent event = db.insertFile(newFolder);

            // Then complete the response
            response.addProperty("newFolderId", newFolder.getID());
            response.addProperty("created", true);

            // But first, send a broadcast message to advise the other
            // client that a new folder is created

            // The notification will contain the new file information
            emitter.emitEvent(event);
        } catch (Exception ex) {

            log.warn(ex);
            response.addProperty("created", false);
        }

        // Finally return the response
        return response;
    }
}