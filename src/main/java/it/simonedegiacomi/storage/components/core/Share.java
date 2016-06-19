package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class Share implements GBModule {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(Search.class);

    private final Gson gson = MyGsonBuilder.create();

    /**
     * Event emitter to notify other clients
     */
    private EventEmitter eventEmitter;

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database share table
     */
    private Dao<Sharing, Long> shareTable;

    /**
     * Database event table
     */
    private Dao<SyncEvent, Long> eventTable;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        eventEmitter = env.getEmitter();
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
            shareTable = DaoManager.createDao(env.getDbConnection(), Sharing.class);
        } catch (SQLException ex) {
            log.warn("Unable to create dao", ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "share")
    public JsonElement onShareQuery(JsonElement data) {

        log.info("New share query");

        // Prepare the response
        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        if (!request.has("ID")) {
            response.addProperty("success", false);
            response.addProperty("error", "Missing file id");
            return response;
        }

        // Check if the file is to share or unshare
        boolean share = request.has("share") && request.get("share").getAsBoolean();

        try {

            // Get the database file
            GBFile dbFile = DBCommonUtils.getFileById(fileTable, request.get("ID").getAsLong());

            // Assert that the file exists
            if (dbFile == null) {
                response.addProperty("success", false);
                response.addProperty("error", "file doesn't exist");
                return response;
            }

            // Check if the file is already shared
            boolean shared = DBCommonUtils.isFileSharedByFileId(shareTable, dbFile.getID());

            // Is the file already in the request share state?
            if (shared == share) {
                response.addProperty("success", false);
                response.addProperty("error", "Already with this access restriction");
                return response;
            }

            SyncEvent event;

            if (share) {
                shareTable.create(new Sharing(dbFile));
                event = new SyncEvent(SyncEvent.EventKind.FILE_SHARED, dbFile);
            } else {
                DeleteBuilder<Sharing, Long> stmt = shareTable.deleteBuilder();
                stmt.where().eq("file_ID", dbFile.getID());
                stmt.delete();

                event = new SyncEvent(SyncEvent.EventKind.FILE_UNSHARED, dbFile);
            }

            // Register and emit the event
            eventTable.create(event);
            eventEmitter.emitEvent(event);

            // Complete the response
            response.addProperty("success", true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }

        return response;
    }

    @WSQuery(name = "getSharedFiles")
    public JsonElement onShareListQuery(JsonElement data) {

        log.info("New shared file list query");

        // Prepare the response
        JsonObject response = new JsonObject();

        try {

            // Prepare the query
            QueryBuilder<GBFile, Long> fileQuery = fileTable.queryBuilder();
            QueryBuilder<Sharing, Long> sharingQuery = shareTable.queryBuilder();
            List<GBFile> sharedFiles =  fileQuery.join(sharingQuery).query();

            // Fill the response
            response.add("files", gson.toJsonTree(sharedFiles, new TypeToken<List<GBFile>>(){}.getType()));
            response.addProperty("found", true);
            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("found", false);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }

        return response;
    }
}