package it.simonedegiacomi.storage.handlers.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.simonedegiacomi.storage.StorageDB;

import java.io.IOException;

/**
 * @author Degiacomi Simone
 * Created on 22/02/16.
 */
public class ToStorageHttpHandler implements HttpHandler {

    private StorageDB db;

    public ToStorageHttpHandler (StorageDB db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if(!httpExchange.getRequestMethod().equals("POST")) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.close();
            return;
        }

    }
}
