package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;

import java.util.logging.Logger;

/**
 * This handler is called to get the path of a specific
 * file given his id
 *
 * Created by Degiacomi Simone on 07/02/16.
 */
public class FindPathHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(FindPathHandler.class.getName());


    private final Gson gson = new Gson();

    private final StorageDB db;

    public FindPathHandler (StorageDB db) {
        this.db = db;
    }

    @WSQuery(name = "whatPath")
    @Override
    public JsonElement onQuery (JsonElement data) {
        // Wrap the file
        GBFile file = gson.fromJson(data, GBFile.class);
        try {
            db.findPath(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return gson.toJsonTree(file.getPathAsList(), GBFile.class);
    }
}
