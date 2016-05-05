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
 * Created on 21/02/16.
 */
public class ShareListHandler implements WSQueryHandler{

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(ShareListHandler.class);

    /**
     * Database
     */
    private final StorageDB db;

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    public ShareListHandler (StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        this.db = env.getDB();
    }

    @WSQuery(name = "getSharedFiles")
    @Override
    public JsonElement onQuery(JsonElement data) {

        // Prepare the response
        JsonObject response = new JsonObject();

        try {
            List<GBFile> files = db.getSharedList();
            response.add("files", gson.toJsonTree(files, new TypeToken<List<GBFile>>(){}.getType()));
            response.addProperty("found", true);
            response.addProperty("success", true);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("found", false);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }

        return response;
    }
}