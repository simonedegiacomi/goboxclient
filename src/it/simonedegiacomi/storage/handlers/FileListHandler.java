package it.simonedegiacomi.storage.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;

import java.util.logging.Logger;

/**
 * This handler return the list of the children of
 * the folder specified by id
 *
 * Created by Degiacomi Simone on 07/02/16.
 */
public class FileListHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(FileListHandler.class.getName());

    private final StorageDB db;

    private final Gson gson = new Gson();

    public FileListHandler (StorageDB db) {
        this.db = db;
    }

    @WSQuery(name = "listFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("ListFile query");
        try {
            GBFile father = gson.fromJson(data, GBFile.class);

            GBFile detailedFile = db.getFileById(father.getID(), false, true);

            return gson.toJsonTree(detailedFile, GBFile.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}