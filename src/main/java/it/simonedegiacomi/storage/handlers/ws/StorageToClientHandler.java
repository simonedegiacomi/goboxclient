package it.simonedegiacomi.storage.handlers.ws;

import com.google.common.collect.Range;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.sender.HttpUrlConnectionDestination;
import it.simonedegiacomi.storage.sender.Sender;
import it.simonedegiacomi.storage.sender.SenderDestination;
import it.simonedegiacomi.storage.utils.MyRange;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;

/**
 * This handler receive the incoming file from the client
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 */
public class StorageToClientHandler implements WSQueryHandler {

    private static final Logger log = Logger.getLogger(StorageToClientHandler.class.getName());

    /**
     * Configuration of the program. This is used to get the urls and the
     * auth token
     */
    private final Config config = Config.getInstance();

    /**
     * Urls used to get the url to which make the request
     */
    private final URLBuilder urls = config.getUrls();

    /**
     * Auth object used to authorize the http connection
     */
    private final Auth auth = config.getAuth();

    /**
     * Path of the files folder on this machine
     */
    private final String PATH = config.getProperty("path");

    /**
     * Database of the storage
     */
    private final StorageDB db;

    /**
     * Onject that send the files to the storage trough a http request
     */
    private final Sender sender = new Sender();

    public StorageToClientHandler(StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "sendMeTheFile")
    @Override
    public JsonElement onQuery(JsonElement data) {

        log.info("New download request");

        // Prepare the response
        JsonObject response = new JsonObject();

        // Get the id of the file and the key of the download
        JsonObject jsonData = (JsonObject) data;

        // Remove the field id to reuse this object. This is done just to not create a new json object.
        // Doing this the data object contains the downloadKey
        long fileID = jsonData.remove("ID").getAsLong();

        // Check if the download is authorized
        boolean authorized = jsonData.remove("authorized").getAsBoolean();

        // Check if the client wants a preview version of the file
        boolean preview = jsonData.has("preview") ? jsonData.get("preview").getAsBoolean() : false;

        try {

            // If the download is not authorized, check of the file is shared
            if(!authorized && !db.isShared(new GBFile(fileID))) {

                // Unauthorized download
                response.addProperty("error", "unauthorized");
                response.addProperty("success", false);
                return response;
            }

            // Get the file from the database
            GBFile dbFile = db.getFileById(fileID, true, false);

            // Set the prefix of this file
            dbFile.setPrefix(PATH);

            // Create the url to upload the file
            URL url = new URL(urls.getAsString("sendFileToClient") + "?downloadKey=" + jsonData.get("downloadKey").getAsString());

            // Create a connection from the url
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Configure the connection
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            // Authorize the connection
            auth.authorize(conn);

            // Create the dst object
            SenderDestination dst = new HttpUrlConnectionDestination(conn);

            // Send the file in the right way
            if(dbFile.isDirectory()) { // If it's a directory

                sender.sendDirectory(dbFile, dst);
            } else if (preview) { // If the client only want the preview

                sender.sendPreview(dbFile, dst);
            } else { // File

                // Check if the client has specified the range
                if(jsonData.has("range") && jsonData.get("range").getAsString().length() > 0) {

                    // Parse the range from the string
                    Range range = MyRange.parse(jsonData.get("range").getAsString());

                    sender.sendFile(dbFile, dst, range);
                } else {

                    // Just send the file
                    sender.sendFile(dbFile, dst);
                }
            }

            // Flush and close the connection
            int responseCode = conn.getResponseCode();
            conn.disconnect();

            // Complete the response
            response.addProperty("success", responseCode == 200);

            log.info("File sent to the client");
        } catch (StorageException ex) {

            log.warn(ex);
            response.addProperty("error", "file not found");
            response.addProperty("error", 404);
            response.addProperty("success", false);
        } catch (IOException ex) {

            log.warn(ex);
            response.addProperty("error", "storage error");
            response.addProperty("success", false);
            response.addProperty("error", 500);
        }

        return response;
    }
}