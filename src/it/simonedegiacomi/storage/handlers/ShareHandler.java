package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.utils.MyGson;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class ShareHandler implements WSQueryHandler {

    private final Gson gson = new MyGson().create();

    private final StorageDB db;

    public ShareHandler (StorageDB db) {
        this.db = db;
    }

    @WSQuery(name = "share")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject response = new JsonObject();
        JsonObject request = (JsonObject) data;
        boolean share = response.has("share") ? request.get("share").getAsBoolean() : false;
        try {
            GBFile file = gson.fromJson(request.get("file"), GBFile.class);
            db.changeAccess(file, share);
            response.addProperty("success", true);
        } catch (Exception ex) {
            response.addProperty("success", false);
        }
        return response;
    }
}
