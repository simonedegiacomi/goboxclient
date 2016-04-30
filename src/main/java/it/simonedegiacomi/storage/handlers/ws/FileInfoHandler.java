package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import org.apache.log4j.Logger;

/**
 * This handler return some information about a specific file. You can specify what
 * you want in the query. To make this work you need to provide the id of the file or
 * at least the name with the path
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 */
public class FileInfoHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(FileInfoHandler.class.getName());

    private final StorageDB db;

    private final Gson gson = new MyGsonBuilder().create();

    public FileInfoHandler(StorageEnvironment env) {
        this.db = env.getDB();
    }
    
    @WSQuery(name = "info")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("File Info query");

        JsonObject res = new JsonObject();
        JsonObject json = (JsonObject) data;
        boolean findPath = json.has("findPath") ? json.get("findPath").getAsBoolean() : false;
        boolean findChildren = json.has("findChildren") ? json.get("findChildren").getAsBoolean() : true;

        try {
            // Wrap the father from the request
            GBFile file = gson.fromJson(json.get("file"), GBFile.class);

            // If who make the query is not authenticated
            if (json.has("public") && json.get("public").getAsBoolean()) {

                // Check if the file is shared
                if (!db.isShared(file)) {

                    res.addProperty("found", false);
                    return res;
                }
            }

            // Get the file form the database
            GBFile dbFile = db.getFile(file);

            // Check if the file exists
            if (dbFile == null) {
                res.addProperty("found", false);
                return res;
            }

            if (findChildren)
                db.findChildren(dbFile);

            if (findPath)
                db.findPath(dbFile);

            res.add("file", gson.toJsonTree(dbFile, GBFile.class));
            res.addProperty("found", true);
        } catch (StorageException ex) {
            ex.printStackTrace();
            res.addProperty("found", false);
        }
        return res;
    }
}