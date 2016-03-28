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
import it.simonedegiacomi.storage.InternalClient;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import org.apache.log4j.Logger;

import java.nio.file.Files;

/**
 * This hanle the request to create new directories
 *
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class CreateFolderHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(CreateFolderHandler.class.getName());

    private final StorageDB db;

    private final EventEmitter emitter;

    private final InternalClient internalClient;

    private final String PATH = Config.getInstance().getProperty("path");

    private final Gson gson = new Gson();

    public CreateFolderHandler (StorageEnvironment env) {
        this.db = env.getDB();
        this.emitter = env.getEmitter();
        this.internalClient = env.getInternalClient();
    }

    @WSQuery(name = "createFolder")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("CreateFolder query");
        // Wrap the new directory
        GBFile newFolder = gson.fromJson(data, GBFile.class);

        // Prepare the response
        JsonObject response = new JsonObject();
        try {
            // Find the right path of the new folder
            if(newFolder.getPathAsList() == null)
                db.findPath(newFolder);

            // Check if another file with the same name already exists
            if(Files.exists(newFolder.toFile(PATH).toPath())) {
                response.addProperty("created", false);
                return response;
            }

            // Tell the internal client ot ignore this event
            internalClient.ignore(newFolder);

            // Create the real file in the FS
            Files.createDirectory(newFolder.toFile(PATH).toPath());

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
            ex.printStackTrace();
            response.addProperty("created", false);
        }

        // Finally return the response
        return response;
    }
}