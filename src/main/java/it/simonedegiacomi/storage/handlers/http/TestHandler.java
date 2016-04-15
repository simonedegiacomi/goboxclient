package it.simonedegiacomi.storage.handlers.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Created on 15/04/16.
 * @author Degiacomi Simone
 */
public class TestHandler implements HttpHandler {

    /**
     * Return a sample and static response
     * @param httpExchange
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Send a static and simple response
        httpExchange.sendResponseHeaders(200, 0);
        httpExchange.getResponseBody().write("It works!".getBytes());

        // Close the request
        httpExchange.close();
    }
}
