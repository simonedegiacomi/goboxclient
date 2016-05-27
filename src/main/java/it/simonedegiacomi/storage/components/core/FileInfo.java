package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class FileInfo implements GBComponent {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(GBComponent.class);

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Database table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database sharing table
     */
    private Dao<Sharing, Long> shareTable;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            shareTable = DaoManager.createDao(env.getDbConnection(), Sharing.class);
        } catch (SQLException ex) {
            throw new AttachFailException("Cannot get file database table");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "info")
    public JsonElement onFileInfoQuery (JsonElement data) {

        log.info("New file info query");

        // Prepare the response
        JsonObject response = new JsonObject();
        JsonObject request = data.getAsJsonObject();

        boolean publicAccess = request.has("public") && request.get("public").getAsBoolean();
        boolean findPath = request.has("findPath") && request.get("findPath").getAsBoolean();
        boolean findChildren = request.has("findChildren") && request.get("findChildren").getAsBoolean();

        // Create the poor file reference
        GBFile poorFile = gson.fromJson(request.get("file"), GBFile.class);

        try {

            // Get the file from the database
            GBFile dbFile = DBCommonUtils.getFile(fileTable, poorFile);

            // Check if the file exists
            if (dbFile == null) {
                response.addProperty("success", false); //TODO: change success flag to true
                response.addProperty("found", false);
                response.addProperty("error", "File not found");
                return response;
            }

            // Check if the file is shared if the request is unauthorized
            if (publicAccess && !DBCommonUtils.isFileSharedByFileId(shareTable, dbFile.getID())) {
                response.addProperty("success", false);
                response.addProperty("found", true);
                response.addProperty("error", "File is not shared");
            }

            // Find children if needed
            if (findChildren) {
                DBCommonUtils.findChildren(fileTable, dbFile);
            }

            // Find path if needed
            if (findPath) {
                DBCommonUtils.findPath(fileTable, dbFile);
            }

            // Complete the response
            response.addProperty("success", true);
            response.addProperty("found", true);
            response.add("file", gson.toJsonTree(dbFile, GBFile.class));
        } catch (SQLException ex) {
            response.addProperty("success", false);
            response.addProperty("found", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }
}