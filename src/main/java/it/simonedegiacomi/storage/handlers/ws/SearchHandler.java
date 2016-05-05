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
import it.simonedegiacomi.storage.StorageException;
import org.apache.log4j.Logger;

import java.security.InvalidParameterException;
import java.util.List;

/**
 * @author Degiacomi Simone
 * Created  on 12/02/16.
 */
public class SearchHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(SearchHandler.class);

    /**
     * Gson
     */
    private final Gson gson = new MyGsonBuilder().create();

    /**
     * Database
     */
    private final StorageDB db;

    public SearchHandler (StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        this.db = env.getDB();
    }

    @WSQuery(name = "search")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject req = (JsonObject) data;

        // Prepare the response
        JsonObject response = new JsonObject();

        // Get the search parameters from the json
        String keyword = req.has("keyword") ? req.get("keyword").getAsString() : new String();
        String kind = req.has("kind") ? req.get("kind").getAsString() : null;
        if(kind.equals("any"))
            kind = new String();
        long from = req.has("from") ? req.get("from").getAsLong() : 0;
        long n = req.has("size") ? req.get("size").getAsLong() : -1;

        try{
            // Get the search result
            List<GBFile> resultList = db.search(keyword, kind, from, n);
            response.add("result", gson.toJsonTree(resultList, new TypeToken<List<GBFile>>(){}.getType()));

            // Specify that there was no error
            response.addProperty("success", true);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }
}