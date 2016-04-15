package it.simonedegiacomi.storage.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class HttpUrlConnectionDestination implements SenderDestination {

    private HttpURLConnection conn;

    public HttpUrlConnectionDestination(HttpURLConnection conn) {

        this.conn = conn;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {

        return conn.getOutputStream();
    }

    @Override
    public void setHeader(String headerName, String headerValue) {

        conn.addRequestProperty(headerName, headerValue);
    }
}
