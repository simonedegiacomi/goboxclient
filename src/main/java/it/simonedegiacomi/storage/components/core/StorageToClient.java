package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.HttpRequest;
import org.apache.log4j.Logger;

import javax.xml.ws.spi.http.HttpExchange;
import java.sql.SQLException;

/**
 * This component receive incoming file from bridge connected clients.
 *
 * Created on 26/05/16.
 * @author Degiacomi Simone
 */
public class ClientToStorage implements GBComponent {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(ClientToStorage.class);

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

    /**
     * This method is called when a new download request is made by a client in bridge mode
     * @param data
     */
    @WSQuery(name = "comeToGetTheFile")
    private JsonElement onBridgeDownloadRequest (JsonElement data) {

        log.info("New download bridge request");

        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        // Assert that the request is valid
        if (!request.has("uploadKey") || !request.has("name") || !request.has("fatherId") || !request.has("path")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing parameters");
            response.addProperty("httpCode", 400);
            return response;
        }

        // Create the file representation
        GBFile file = gson.fromJson(data, GBFile.class);

        try {
            // Check in the database if the file already exists
            boolean replace = DBUtils.getFile(fileTable, file);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            response.addProperty("httpCode", 500);
            return response;
        }
        response.addProperty("success", true);
        return response;
    }

    @HttpRequest(name = "", method = "GET")
    private void onDirectDownloadRequest (HttpExchange req) {

    }
}