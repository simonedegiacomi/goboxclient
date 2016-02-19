package it.simonedegiacomi.storage.handlers;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.annotations.WSEvent;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.utils.MyZip;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This handler receive the incoming file from the client
 *
 * @author Degiacomi Simone
 * Created on 07/02/16.
 */
public class StorageToClientHandler implements WSEventListener {

    private static final Logger log = Logger.getLogger(StorageToClientHandler.class.getName());

    /**
     * Configuration of the program. This is used to get the urls and the
     * auth token
     */
    private final Config config = Config.getInstance();

    private final URLBuilder urls = config.getUrls();

    private final Auth auth = config.getAuth();

    /**
     * Path of the files folder on this machine
     */
    private final String PATH = config.getProperty("path");

    /**
     * Database of the storage
     */
    private final StorageDB db;

    public StorageToClientHandler(StorageDB db) {
        this.db = db;
    }

    @WSEvent(name = "sendMeTheFile")
    @Override
    public void onEvent(JsonElement data) {


        // Get the ID. Check if is the request is authorized. If is not, check if the file
        // is shared.
        // Check if the file is a single file or if it's a folder. If it's a folder, send a zip of the folder




        log.info("New download request");

        // Get the id of the file and the key of the download
        JsonObject jsonData = (JsonObject) data;
        // Remove the field id to reuse this object. This is done just to
        // not create a new json object. Doing this the data object contains the
        // downloadKey
        long fileID = jsonData.remove("ID").getAsLong();

        // Check if the download is authorized
        boolean authorized = jsonData.remove("authorized").getAsBoolean();

        try {

            // If the file should be shared, check if it is
            if(!authorized && !db.isShared(new GBFile(fileID)))
                // TODO: Send to the server an error
                return;

            // Get the file from the database
            GBFile dbFile = db.getFileById(fileID, true, false);

            // Create the url to upload the file
            URL url = new URL(urls.getAsString("sendFileToClient") + "?downloadKey=" + jsonData.get("downloadKey").getAsString());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            auth.authorize(conn);


            // Get the output stream to the server
            OutputStream rawStreamToServer = conn.getOutputStream();

            // Send the file
            if(dbFile.isDirectory()) {
                MyZip.zipFolder(dbFile.toFile(PATH), rawStreamToServer);
            } else {
                // Get the connection stream
                DataOutputStream toServer = new DataOutputStream(rawStreamToServer);

                // Open the file
                DataInputStream fromFile = new  DataInputStream(new FileInputStream(dbFile.toFile(PATH)));

                // Send the file
                ByteStreams.copy(fromFile, toServer);

                // Close the streams
                toServer.close();
                fromFile.close();
            }

            // Flush and close the connection
            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}