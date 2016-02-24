package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.utils.MyGson;

import java.util.List;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class ShareListHandler implements WSQueryHandler{

    private final StorageDB db;

    private final Gson gson = new MyGson().create();

    public ShareListHandler (StorageDB db ) {
        this.db = db;
    }

    @WSQuery(name = "getSharedFiles")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject response = new JsonObject();

        try {

            List<GBFile> files = db.getSharedFiles();

            response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>(){}.getType()));

            response.addProperty("found", true);
        } catch (Exception ex) {
            response.addProperty("found", false);
        }

        return response;
    }
}
