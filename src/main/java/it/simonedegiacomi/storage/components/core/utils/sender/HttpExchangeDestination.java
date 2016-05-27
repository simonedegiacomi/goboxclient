package it.simonedegiacomi.storage.components.core.utils.sender;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class HttpExchangeDestination implements SenderDestination {

    /**
     * Raw http exchange objecy
     */
    private final HttpExchange conn;
    private final Headers headers;

    public HttpExchangeDestination (HttpExchange conn) {

        this.conn = conn;
        this.headers = conn.getResponseHeaders();
    }

    @Override
    public OutputStream getOutputStream() {

        return conn.getResponseBody();
    }

    @Override
    public void setHeader(String headerName, String headerValue) {
        headers.add(headerName, headerValue);
    }

    @Override
    public void sendHeaders(int httpCode) throws IOException {
        conn.sendResponseHeaders(httpCode, 0);
    }
}