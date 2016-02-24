package it.simonedegiacomi.storage;

import com.sun.net.httpserver.HttpsServer;
import it.simonedegiacomi.storage.handlers.http.FromStorageHttpHandler;
import it.simonedegiacomi.storage.handlers.http.ToStorageHttpHandler;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author Degiacomi Simone
 * Created  on 22/02/16.
 */
public class HttpsStorageServer {
    private HttpsServer server;

    public HttpsStorageServer (InetSocketAddress address) throws IOException {
        //server = HttpsServer.create(address, 0);
        //SSLContext sslContext = SSLContext.getInstance("TLS");
        registerHandlers();
    }

    public void serve () {
        server.start();
    }

    private void registerHandlers () {
        server.createContext("/toStorage", new ToStorageHttpHandler());
        server.createContext("/fromStorage", new FromStorageHttpHandler());
    }
}