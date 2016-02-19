package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;

/**
 * Created by simone on 12/02/16.
 */
public class SearchHandler implements WSQueryHandler {

    private final Gson gson = new Gson();

    private final StorageDB db;

    public SearchHandler (StorageDB db) {
        this.db = db;
    }

    @WSQuery(name = "search")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject req = (JsonObject) data;
        JsonObject response = new JsonObject();
        String keyword = req.has("keyword") ? req.get("keyword").getAsString() : null;
        String kind = req.has("kind") ? req.get("kind").getAsString() : null;
        return response;
    }
}
