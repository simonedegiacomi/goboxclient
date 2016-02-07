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

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * This handler receive the incoming file from the client
 *
 * Created by Degiacomi Simone on 07/02/16.
 */
public class ClientToStorageHandler implements WSEventListener {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(ClientToStorageHandler.class.getName());

    /**
     * Configuration of the program. This is used to get the urls and the
     * auth object
     */
    private final Config config = Config.getInstance();

    private final URLBuilder urls = config.getUrls();

    private final Auth auth = config.getAuth();

    /**
     * Path of the files folder
     */
    private final String PATH = config.getProperty("path");

    /**
     * Database of the storage
     */
    private final StorageDB db;

    public ClientToStorageHandler (StorageDB db) {
        this.db = db;
    }

    @WSEvent(name = "sendMeTheFile")
    @Override
    public void onEvent(JsonElement data) {
        log.info("New download request");
        try {
            JsonObject jsonData = (JsonObject) data;
            String downloadKey = jsonData.get("downloadKey").getAsString();
            long file = jsonData.get("ID").getAsLong();

            // get the file from the database
            GBFile dbFile = db.getFileById(file);

            jsonData.remove("ID");

            URL url = urls.get("sendFileToClient", data);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            auth.authorize(conn);

            conn.setDoOutput(true);

            DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());

            // Open the file
            DataInputStream fromFile = new  DataInputStream(new FileInputStream(dbFile.getPathAsString(PATH)));

            // Send the file
            ByteStreams.copy(fromFile, toServer);

            fromFile.close();
            toServer.close();
            System.out.println(conn.getResponseCode());
            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}