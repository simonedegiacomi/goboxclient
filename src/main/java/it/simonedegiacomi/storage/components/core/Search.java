package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
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
 * @author Degiacomi simone
 */
public class Search implements GBModule {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(Search.class);

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "search")
    public JsonElement onQuery(JsonElement data) {

        log.info("New search query");

        // Prepare the response
        JsonObject req = (JsonObject) data;
        JsonObject response = new JsonObject();

        // Get the search parameters from the json
        String keyword = req.has("keyword") ? req.get("keyword").getAsString() : "";
        String kind = req.has("kind") ? req.get("kind").getAsString() : null;
        if(kind.equals("any"))
            kind = "";
        long from = req.has("from") ? req.get("from").getAsLong() : 0;
        long n = req.has("size") ? req.get("size").getAsLong() : -1;

        try{
            // Get the search result
            List<GBFile> resultList = DBCommonUtils.search(fileTable, keyword, kind, from, n);
            response.add("result", gson.toJsonTree(resultList, new TypeToken<List<GBFile>>(){}.getType()));

            // Specify that there was no error
            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
        }
        return response;
    }
}
