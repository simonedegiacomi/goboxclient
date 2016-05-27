package it.simonedegiacomi.storage.components.core;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.HttpRequest;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.storage.components.core.utils.sender.HttpUrlConnectionDestination;
import it.simonedegiacomi.storage.components.core.utils.sender.Sender;
import it.simonedegiacomi.storage.components.core.utils.sender.SenderDestination;
import it.simonedegiacomi.storage.utils.MyRange;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.ws.spi.http.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This component sends a file received from bridge client to the storage
 *
 * Created on 26/05/16.
 * @author Degiacomi Simone
 */
public class StorageToClient implements GBComponent {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(StorageToClient.class);

    /**
     * Url builder
     */
    private final URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * Gson
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * File and folder sender
     */
    private final Sender sender = new Sender();

    /**
     * User credentials
     */
    private GBAuth auth;

    /**
     * Prefix of the environment files folder
     */
    private String PATH;

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database event table
     */
    private Dao<SyncEvent, Long> eventTable;

    /**
     * Event emitter
     */
    private EventEmitter eventEmitter;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        eventEmitter = env.getEmitter();
        auth = env.getGlobalConfig().getAuth();
        PATH = env.getGlobalConfig().getProperty("path", "files/");
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
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
    public JsonElement onBridgeDownloadRequest (JsonElement data) {

        log.info("New download bridge request");

        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        // Assert that the request is valid
        if (!request.has("uploadKey") || !request.has("id")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing parameters");
            response.addProperty("httpCode", 400);
            return response;
        }

        boolean thumbnail = request.has("preview") && request.get("preview").getAsBoolean();

        // Create the file representation
        GBFile file = gson.fromJson(data, GBFile.class);

        try {

            // Check in the database if the file exists
            GBFile dbFile = DBCommonUtils.getFile(fileTable, file);

            if (dbFile == null) {
                response.addProperty("success", false);
                response.addProperty("error", "File not found");
                response.addProperty("success", 404);
                return response;
            }

            // Set the environment path
            dbFile.setPrefix(PATH);

            // Create a connection from the url with the upload key as parameter
            JsonObject params = new JsonObject();
            params.addProperty("uploadKey", request.get("uploadKey").getAsString());
            HttpsURLConnection conn = (HttpsURLConnection) urls.get("sendFileToClient", params).openConnection();

            // Configure the connection
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            // Authorize the connection
            auth.authorize(conn);

            // Create the destination object
            SenderDestination dst = new HttpUrlConnectionDestination(conn);

            if (file.isDirectory()) {
                sender.sendDirectory(file, dst);
            } else if (thumbnail) {
                sender.sendPreview(file, dst);
            } else {
                // Check if the client has specified the range
                if(request.has("range") && request.get("range").getAsString().length() > 0) {

                    // Parse the range from the string
                    Range range = MyRange.parse(request.get("range").getAsString());

                    sender.sendFile(dbFile, dst, range);
                } else {

                    // Just send the file
                    sender.sendFile(dbFile, dst);
                }
            }

            /// Create the view event
            if (!thumbnail) {
                SyncEvent view = new SyncEvent(SyncEvent.EventKind.FILE_OPENED, dbFile);
                view.setDate(System.currentTimeMillis());
                eventTable.create(view);
            }


        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            response.addProperty("httpCode", 500);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        response.addProperty("success", true);
        return response;
    }

    @HttpRequest(name = "", method = "GET")
    public void onDirectDownloadRequest (HttpExchange req) {

    }
}