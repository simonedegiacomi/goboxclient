package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;

import java.util.logging.Logger;

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

    private final Gson gson = new Gson();

    private final String PATH = Config.getInstance().getProperty("path");

    public FileInfoHandler(StorageDB db) {
        this.db = db;
    }
    
    @WSQuery(name = "info")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("File Info query");

        JsonObject res = new JsonObject();
        JsonObject json = (JsonObject) data;
        boolean findPath = json.has("findPath") ? json.get("findPath").getAsBoolean() : false;
        boolean findChildren = json.has("findChildren") ? json.get("findChildren").getAsBoolean() : false;

        // Wrap the father from the request
        GBFile file = gson.fromJson(json.get("file"), GBFile.class);
        try {
            if(file.getID() == GBFile.UNKNOWN_ID)
                db.findIDByPath(file);

            GBFile detailedFile = db.getFileById(file.getID(), findPath, findChildren);

            JsonElement detailedJsonFile =  gson.toJsonTree(detailedFile, GBFile.class);

            res.add("file", detailedJsonFile);
            res.addProperty("found", true);
        } catch (Exception ex) {
            ex.printStackTrace();
            res.addProperty("found", false);
        }
        return res;
    }
}