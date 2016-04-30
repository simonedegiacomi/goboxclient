package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;

import java.util.List;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class ShareListHandler implements WSQueryHandler{

    private final StorageDB db;

    private final Gson gson = MyGsonBuilder.create();

    public ShareListHandler (StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "getSharedFiles")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject response = new JsonObject();

        try {
            List<GBFile> files = db.getSharedList();
            response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>(){}.getType()));
            response.addProperty("found", true);
        } catch (Exception ex) {
            response.addProperty("found", false);
        }

        return response;
    }
}