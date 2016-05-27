package it.simonedegiacomi.storage.components.core;

import com.google.common.collect.Range;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.HttpRequest;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.storage.components.core.utils.sender.*;
import it.simonedegiacomi.storage.utils.MyRange;
import it.simonedegiacomi.utils.MyHttpExchangeUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;

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
     * Database sharing table
     */
    private Dao<Sharing, Long> shareTable;

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
            shareTable = DaoManager.createDao(env.getDbConnection(), Sharing.class);
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
    @WSQuery(name = "sendMeTheFile")
    public JsonElement onBridgeDownloadRequest (JsonElement data) {

        log.info("New download bridge request");

        JsonObject request = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        // Assert that the request is valid
        if (!request.has("downloadKey") || !request.has("ID")) {
            log.warn("Client request miss parameters");
            response.addProperty("success", false);
            response.addProperty("error", "missing parameters");
            response.addProperty("httpCode", 400);
            return response;
        }

        boolean thumbnail = request.has("preview") && request.get("preview").getAsBoolean();
        boolean authorized = request.has("authorized") && request.get("authorized").getAsBoolean();

        try {

            // Check in the database if the file exists
            GBFile dbFile = DBCommonUtils.getFileById(fileTable, request.get("ID").getAsLong());

            // Check if the file exists
            if (dbFile == null) {
                log.warn("File not found");
                response.addProperty("success", false);
                response.addProperty("error", "File not found");
                response.addProperty("success", 404);
                return response;
            }

            // Check if the client is authorized to access the file
            if (!authorized && !DBCommonUtils.isFileSharedByFileId(shareTable, dbFile.getID())) {
                log.info("Unauthorized download");
                response.addProperty("success", false);
                response.addProperty("error", "Unauthorized");
                response.addProperty("httpCode", 401);
                return response;
            }

            // Find the path
            DBCommonUtils.findPath(fileTable, dbFile);

            // Set the environment path
            dbFile.setPrefix(PATH);

            // Create a connection from the url with the upload key as parameter

            // TODO: Fix url parameters builder
            //JsonObject params = new JsonObject();
            //params.addProperty("downloadKey", request.get("downloadKey").getAsString());
            //HttpsURLConnection conn = (HttpsURLConnection) urls.get("sendFileToClient", params).openConnection();
            HttpsURLConnection conn = (HttpsURLConnection) new URL(urls.get("sendFileToClient").toString() + "?downloadKey=" + request.get("downloadKey").getAsString()).openConnection();

            // Configure the connection
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            // Authorize the connection
            auth.authorize(conn);

            // Create the destination object
            SenderDestination dst = new HttpUrlConnectionDestination(conn);

            // Create and fill the send action
            SendAction action = new SendAction();
            action.setFileToSend(dbFile);
            action.setThumbnail(thumbnail);
            if(request.has("range") && request.get("range").getAsString().length() > 0) {
                action.setRange(MyRange.parse(request.get("range").getAsString()));
            }

            // Send the file
            sender.send(action, dst);

            // Get the response
            if (conn.getResponseCode() != 200) {
                log.warn("File transfer from storage to client failed: " + conn.getResponseMessage());
                response.addProperty("success", false);
                response.addProperty("httpCode", 500);
                response.addProperty("error", conn.getResponseMessage());

                // Close the connection
                conn.disconnect();
                return response;
            }

            log.info("File sent to the client");

            // Close the connection
            conn.disconnect();

            /// Create the view event
            if (!thumbnail) {
                SyncEvent view = new SyncEvent(SyncEvent.EventKind.FILE_OPENED, dbFile);
                view.setDate(System.currentTimeMillis());
                eventTable.create(view);
            }

            // Complete the response
            response.addProperty("success", true);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            response.addProperty("httpCode", 500);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @HttpRequest(name = "/fromStorage")
    public void onDirectDownloadRequest (HttpExchange req) throws IOException {

        log.info("New direct download request");

        // Get the url query params
        Map<String, String> params = MyHttpExchangeUtils.getQueryParams(req.getRequestURI());
        Headers headers = req.getRequestHeaders();

        // Id of the file
        long fileId = Long.parseLong(params.get("ID"));

        // Check if the client want a preview
        boolean thumbnail = params.containsKey("preview") && Boolean.parseBoolean(params.get("preview"));

        try {

            // Check if the file exists
            GBFile file = DBCommonUtils.getFileById(fileTable, fileId);

            if (file == null) {
                log.info("Requested file not found");
                req.sendResponseHeaders(404, 0);
                req.close();
            }

            // Find the path
            DBCommonUtils.findPath(fileTable, file);
            file.setPrefix(PATH);

            // Create and fill the send action
            SendAction action = new SendAction();
            action.setFileToSend(file);
            action.setThumbnail(thumbnail);

            // Check if the range is specified
            if (headers.containsKey("Range")) {

                // Parse and set the range
                action.setRange(MyRange.parse(headers.getFirst("Range")));
            }

            // Send the file
            sender.send(action, new HttpExchangeDestination(req));

            // Close the connection
            req.close();

            log.info("File send to the client");

            // Register action
            SyncEvent open = new SyncEvent(SyncEvent.EventKind.FILE_OPENED, file);
            eventTable.create(open);

        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            req.sendResponseHeaders(500, 0);
            req.getResponseBody().write(ex.toString().getBytes());
            req.close();
        }
    }
}