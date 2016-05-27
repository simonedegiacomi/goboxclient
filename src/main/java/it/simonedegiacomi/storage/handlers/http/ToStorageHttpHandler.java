package it.simonedegiacomi.storage.handlers.http;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.utils.MyHttpExchangeUtils;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Degiacomi Simone
 * Created on 22/02/16.
 * @deprecated
 */
public class ToStorageHttpHandler implements HttpHandler {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(ToStorageHttpHandler.class);

    /**
     * Database
     */
    private StorageDB db;

    /**
     * Json parser
     */
    private final JsonParser parser = new JsonParser();

    /**
     * Gson used to wrap json object to gb file
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * Path of the environment
     */
    private final String PATH = Config.getInstance().getProperty("path");

    private final StorageEnvironment env;

    /**
     * Instance a new handler for incoming file
     * @param env Environment to use
     */
    public ToStorageHttpHandler (StorageEnvironment env) {
        this.db = env.getDB();
        this.env = env;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Assert that the method is 'POST'
        if(!httpExchange.getRequestMethod().equals("POST")) {

            // Client's fault
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.getResponseBody().write("method not allowed".getBytes());
            httpExchange.close();
            return;
        }

        // Get the query string parameters
        Map<String, String> params = MyHttpExchangeUtils.getQueryParams(httpExchange.getRequestURI());

        if (!params.containsKey("json")) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.getResponseBody().write("missing parameters".getBytes());
            httpExchange.close();
            return;
        }

        // Get the json string that describe the upload
        String jsonString = params.get("json");

        // Wrap the string into a real json object
        JsonObject json = parser.parse(jsonString).getAsJsonObject();

        if (!json.has("father") || !json.has("name")) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.getResponseBody().write("missing parameters".getBytes());
            httpExchange.close();
            return;
        }

        // Parse tje json string
        GBFile father = gson.fromJson(json.get("father"), GBFile.class);

        try {

            GBFile dbFather = db.getFile(father, true, true);

            if (dbFather ==  null) {
                httpExchange.sendResponseHeaders(400, 0);
                httpExchange.close();
            }

            // Set the path and generate the child
            dbFather.setPrefix(PATH);
            GBFile newFile = dbFather.generateChild(json.get("name").getAsString(), false);
            env.getSync().getFileSystemWatcher().startIgnoring(newFile.toFile());

            // Create the output stream to the disk
            OutputStream toDisk = new FileOutputStream(newFile.toFile());

            // Get the stream from the connection
            InputStream fromClient = httpExchange.getRequestBody();

            // Copy the file to disk
            ByteStreams.copy(fromClient, toDisk);

            // Close the streams
            fromClient.close();
            toDisk.close();

            // Insert in the database
            env.getEmitter().emitEvent(db.insertFile(newFile));

            env.getSync().getFileSystemWatcher().stopIgnoring(newFile.toFile());

            httpExchange.sendResponseHeaders(200, 0);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            httpExchange.sendResponseHeaders(500, 0);
        }

        // Close the http connection
        httpExchange.close();
    }
}