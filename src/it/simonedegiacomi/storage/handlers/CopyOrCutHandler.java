package it.simonedegiacomi.storage.handlers;

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

import java.nio.file.Files;

/**
 * @author Degiacomi Simone
 * Created on 14/02/16.
 */
public class CopyOrCutHandler implements WSQueryHandler {

    private final StorageDB db;

    private final InternalClient internalClient;

    private final EventEmitter emitter;

    private final String PATH = Config.getInstance().getProperty("path");

    private final Gson gson = new Gson();

    public CopyOrCutHandler(StorageDB db, EventEmitter emitter, InternalClient internalClient) {
        this.db = db;
        this.internalClient = internalClient;
        this.emitter = emitter;
    }

    @WSQuery(name = "copyOrCutFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject response = new JsonObject();
        JsonObject json = (JsonObject) data;
        GBFile file = gson.fromJson(((JsonObject) data).get("file"), GBFile.class);
        GBFile newFather = gson.fromJson(((JsonObject) data).get("destination"), GBFile.class);
        GBFile newFile = new GBFile(file.getID(), newFather.getID(), file.getName(), file.isDirectory());
        boolean cut = json.get("cut").getAsBoolean();
        try {
            // Copy the file to the new destination
            Files.copy(file.toFile(PATH).toPath(), newFile.toFile(PATH).toPath());
            internalClient.ignore(newFile);
            SyncEvent creationEvent = db.insertFile(newFile);
            emitter.emitEvent(creationEvent);

            // If the event was an cut operation, delete the older file
            if(cut) {
                internalClient.ignore(file);
                Files.delete(file.toFile(PATH).toPath());
                SyncEvent delete = db.removeFile(file);
                emitter.emitEvent(delete);
            }

            response.addProperty("success", true);
        } catch (Exception ex) {
            response.addProperty("success", false);
        }
        return response;
    }
}
