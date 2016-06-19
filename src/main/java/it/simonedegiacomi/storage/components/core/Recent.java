package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class Recent implements GBModule {

    /**
     * Logger of the class
     */
    private final Logger log  = Logger.getLogger(Recent.class);

    private final Gson gson = MyGsonBuilder.create();

    private Dao<SyncEvent, Long> eventTable;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        try {
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
        } catch (SQLException ex) {
            log.warn("Unable to create dao", ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "recent")
    public JsonElement onQuery(JsonElement data) {

        log.info("New recent query");

        // Prepare the response
        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        // Get the parameters from the request
        long from = request.has("from") ? request.get("from").getAsLong() : 0;
        long size = request.has("size") ? request.get("size").getAsLong() : 50;

        try {
            // Query the database
            List<SyncEvent> events = eventTable.queryBuilder()
                    .orderBy("date", false)
                    .offset(from)
                    .limit(size)
                    .query();

            // Add the files list
            response.add("events", gson.toJsonTree(events, new TypeToken<List<SyncEvent>>(){}.getType()));

            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }

        return response;
    }
}