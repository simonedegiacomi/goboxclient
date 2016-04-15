package it.simonedegiacomi.storage.handlers.http;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.utils.MyHttpExchangeUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Degiacomi Simone
 * Created on 22/02/16.
 */
public class ToStorageHttpHandler implements HttpHandler {

    private StorageDB db;

    private final Gson gson = MyGsonBuilder.create();

    private final String PATH = Config.getInstance().getProperty("path");

    public ToStorageHttpHandler (StorageDB db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Assert that the method is 'POST'
        if(!httpExchange.getRequestMethod().equals("POST")) {

            // Client's fault
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.close();
            return;
        }

        // Get the query string parameters
        Map<String, String> params = MyHttpExchangeUtils.getQueryParams(httpExchange.getRequestURI().toURL());

        // Get the json string that describe the upload
        String jsonString = params.get("json");

        // Parse tje json string
        GBFile incomingFile = gson.fromJson(jsonString, GBFile.class);

        // Set the path of the environment
        incomingFile.setPrefix(PATH);

        try {

            // Calculate the path
            db.findPath(incomingFile);

            // Create the output stream to the disk
            OutputStream toDisk = new FileOutputStream(incomingFile.toFile());

            // Get the stream from the connection
            InputStream fromClient = httpExchange.getRequestBody();

            // Copy the file to disk
            ByteStreams.copy(fromClient, toDisk);

            // Close the streams
            fromClient.close();
            toDisk.close();

        } catch (StorageException ex) {

        }

        // Close the http connection
        httpExchange.close();
    }
}