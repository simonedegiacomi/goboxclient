package it.simonedegiacomi.storage.handlers.ws;

import com.google.common.collect.Range;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.components.core.utils.sender.HttpUrlConnectionDestination;
import it.simonedegiacomi.storage.components.core.utils.sender.Sender;
import it.simonedegiacomi.storage.components.core.utils.sender.SenderDestination;
import it.simonedegiacomi.storage.utils.MyRange;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidParameterException;

/**
 * This handler receive the incoming file from the client
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 * @deprecated
 */
public class StorageToClientHandler implements WSQueryHandler {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(StorageToClientHandler.class);

    /**
     * Configuration of the program. This is used to get the urls and the
     * auth token
     */
    private final Config config = Config.getInstance();

    /**
     * Urls used to get the url to which make the request
     */
    private final URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * Auth object used to authorize the http connection
     */
    private final GBAuth auth = config.getAuth();

    /**
     * Path of the files folder on this machine
     */
    private final String PATH = config.getProperty("path");

    /**
     * Database of the storage
     */
    private final StorageDB db;

    /**
     * Object that send the files to the storage trough a http request
     */
    private final Sender sender = new Sender();

    public StorageToClientHandler(StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        this.db = env.getDB();
    }

    @WSQuery(name = "sendMeTheFile")
    @Override
    public JsonElement onQuery(JsonElement data) {
        log.info("New download request");
        JsonObject jsonData = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        if (!jsonData.has("ID") && !jsonData.has("path")) {
            response.addProperty("error", "file not found");
            response.addProperty("httpCode", 400);
            response.addProperty("success", false);
            return response;
        }

        long ID = jsonData.has("ID") ? jsonData.get("ID").getAsLong() : GBFile.UNKNOWN_ID;

        // Check if the download is authorized
        boolean authorized = jsonData.remove("authorized").getAsBoolean();

        // Check if the client wants a preview version of the file
        boolean preview = jsonData.has("preview") ? jsonData.get("preview").getAsBoolean() : false;

        try {
            GBFile temp = new GBFile(ID);
            if (jsonData.has("path") && jsonData.get("path").getAsString().length() > 0)
                temp.setPathByString(jsonData.get("path").getAsString());
            GBFile dbFile = db.getFile(temp, true, true);

            if (dbFile == null) {

                // File not found
                response.addProperty("error", "file not found");
                response.addProperty("httpCode", 404);
                response.addProperty("success", false);
                return response;
            }

            dbFile.setPrefix(PATH);

            // If the download is not authorized, check of the file is shared
            if(!authorized && !db.isShared(dbFile)) {

                // Unauthorized download
                response.addProperty("error", "unauthorized");
                response.addProperty("httpCode", 401);
                response.addProperty("success", false);
                return response;
            }

            // Create the url to upload the file
            URL url = new URL(urls.get("sendFileToClient").toString() + "?downloadKey=" + jsonData.get("downloadKey").getAsString());

            // Create a connection from the url
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Configure the connection
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            // Authorize the connection
            auth.authorize(conn);

            // Create the destination object
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

            // Register the open action
            db.addToRecent(dbFile);

            log.info("File sent to the client");
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("error", "file not found");
            response.addProperty("httpCode", 404);
            response.addProperty("success", false);
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("error", "storage error");
            response.addProperty("success", false);
            response.addProperty("httpCode", 500);
        }

        return response;
    }
}