package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;

import java.util.List;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class TrashedHandler implements WSQueryHandler {

    private final Gson gson = MyGsonBuilder.create();

    private final StorageDB db;

    public TrashedHandler (StorageEnvironment env) {
        this.db = env.getDB();
    }

    @Override
    public JsonElement onQuery(JsonElement jsonElement) {

        // Prepare the response
        JsonObject response = new JsonObject();

        // Cast the request
        JsonObject request = jsonElement.getAsJsonObject();

        long from = request.has("from") ? request.get("from").getAsLong() : 0;
        long size = request.has("size") ? request.get("size").getAsLong() : 0;

        // Make the query
        List<GBFile> files = db.getTrashedFiles(from, size);

        // Add the files to the response
        response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>(){}.getType()));

        response.addProperty("error", false);

        return response;
    }
}
