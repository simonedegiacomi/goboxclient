package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import org.apache.log4j.Logger;

import java.security.InvalidParameterException;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class ShareHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(ShareHandler.class);

    /**
     * Event emitter to advice all the clients
     */
    private final EventEmitter emitter;

    /**
     * Database
     */
    private final StorageDB db;

    public ShareHandler (StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        if (env.getEmitter() == null)
            throw new InvalidParameterException("environment without event emitter");

        this.db = env.getDB();
        this.emitter = env.getEmitter();
    }

    @WSQuery(name = "share")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject request = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!request.has("ID")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing file id");
            return response;
        }

        // Check if the file is to share or unshare
        boolean share = request.has("share") ? request.get("share").getAsBoolean() : false;

        GBFile file = new GBFile(request.get("ID").getAsLong());
        try {

            // Get the database file
            GBFile dbFile = db.getFile(file);

            // Assert that the file exists
            if (dbFile == null) {
                response.addProperty("success", false);
                response.addProperty("error", "file doesn't exist");
                return response;
            }

            // Change the access of this file
            SyncEvent event = db.share(dbFile, share);

            // Advice all the clients
            emitter.emitEvent(event);

            // Complete the response
            response.addProperty("success", true);
        } catch (StorageException ex) {
            ex.printStackTrace();
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }

        return response;
    }
}