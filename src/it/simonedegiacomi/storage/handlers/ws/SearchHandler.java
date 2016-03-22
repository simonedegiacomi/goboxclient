package it.simonedegiacomi.storage.handlers.ws;

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
 * Created  on 12/02/16.
 */
public class SearchHandler implements WSQueryHandler {

    private final Gson gson = new MyGson().create();

    private final StorageDB db;

    public SearchHandler (StorageDB db) {
        this.db = db;
    }

    @WSQuery(name = "search")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject req = (JsonObject) data;

        // Prepare the response
        JsonObject response = new JsonObject();

        // Get the search parameters from the json
        String keyword = req.has("keyword") ? req.get("keyword").getAsString() : null;
        String kind = req.has("kind") ? req.get("kind").getAsString() : null;
        if(kind.equals("any"))
            kind = new String();
        long from = req.has("from") ? req.get("from").getAsLong() : 0;
        long n = req.has("limit") ? req.get("limit").getAsLong() : -1;

        try{
            // Get the search result
            List<GBFile> resultList = db.search(keyword, kind, from, n);
            response.add("result", gson.toJsonTree(resultList, new TypeToken<List<GBFile>>(){}.getType()));

            // Specify that there was no error
            response.addProperty("error", false);
        } catch (Exception ex) {
            response.addProperty("error", true);
        }
        return response;
    }
}