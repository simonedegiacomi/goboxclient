package it.simonedegiacomi.storage.handlers.http;

import com.google.common.collect.Range;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.sender.HttpExchangeDestination;
import it.simonedegiacomi.storage.sender.Sender;
import it.simonedegiacomi.storage.sender.SenderDestination;
import it.simonedegiacomi.storage.utils.MyRange;
import it.simonedegiacomi.utils.MyHttpExchangeUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Handler that send to the client (connected in direct mode) the files
 * @author Degiacomi Simone
 * Created on 22/02/16.
 */
public class FromStorageHttpHandler implements HttpHandler {

    private final StorageDB db;

    private final Sender sender = new Sender();

    private final String PATH = Config.getInstance().getProperty("path");

    public FromStorageHttpHandler (StorageDB db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Assert that the method is GET
        if(!httpExchange.getRequestMethod().equals("GET")) {

            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.close();
            return;
        }

        try {

            // Get the url query params
            Map<String, String> params = MyHttpExchangeUtils.getQueryParams(httpExchange.getRequestURI().toURL());

            // Id of the file
            long fileId = Long.parseLong(params.get("ID"));

            // check if the client want a preview
            boolean preview = params.containsKey("preview") ? Boolean.parseBoolean(params.get("preview")) : false;

            // Get the file form the database
            GBFile gbFile = db.getFileById(fileId, true, false);

            // Set the environment path
            gbFile.setPrefix(PATH);

            // Create the destination object for the sender
            SenderDestination dst = new HttpExchangeDestination(httpExchange);

            if (gbFile.isDirectory()) {

                sender.sendDirectory(gbFile, dst);
            } else if (preview) {

                sender.sendPreview(gbFile, dst);
            } else {

                Headers headers = httpExchange.getRequestHeaders();
                if (headers.containsKey("Content-Range")) {

                    Range range = MyRange.parse(httpExchange.getRequestHeaders().getFirst("Content-Range"));
                    sender.sendFile(gbFile, dst, range);
                } else {

                    sender.sendFile(gbFile, dst);
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
