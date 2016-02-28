package it.simonedegiacomi.storage.handlers.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.utils.MyGson;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Degiacomi Simone
 * Created on 22/02/16.
 */
public class FromStorageHttpHandler implements HttpHandler {

    private Gson gson = new MyGson().create();

    private final StorageDB db;

    public FromStorageHttpHandler (StorageDB db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if(!httpExchange.getRequestMethod().equals("GET")) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.close();
            return;
        }
        try {
            GBFile fileToDownload = gson.fromJson(new InputStreamReader(httpExchange.getRequestBody()), GBFile.class);
            // find the correct path
            db.findPath(fileToDownload);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
