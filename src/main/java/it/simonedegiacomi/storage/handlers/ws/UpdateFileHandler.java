package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;

/**
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class UpdateFileHandler implements WSQueryHandler{

    private final StorageDB db;

    private final Gson gson = new Gson();

    public UpdateFileHandler (StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "updateFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();
        GBFile file = gson.fromJson(request.get("file"), GBFile.class);
        boolean ovverrideFile = request.get("overrideFile").getAsBoolean();
        try {

            if(ovverrideFile) {
                // Richiamare la procedura di ricezione del file
            }

            SyncEvent update = db.updateFile(file);

            response.addProperty("success", false);
        } catch (Exception ex) {
            response.addProperty("success", false);
        }
        return response;
    }
}