package it.simonedegiacomi.storage.sender;

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

    public HttpExchangeDestination (HttpExchange conn) {

        this.conn = conn;
    }

    @Override
    public OutputStream getOutputStream() {

        return conn.getResponseBody();
    }

    @Override
    public void setHeader(String headerName, String headerValue) {
        conn.getResponseHeaders().add(headerName, headerValue);
    }

    @Override
    public void sendHeaders(int httpCode) throws IOException {
        conn.sendResponseHeaders(httpCode, 0);
    }
}