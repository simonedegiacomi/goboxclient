package it.simonedegiacomi.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsServer;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.handlers.http.DirectLoginHandler;
import it.simonedegiacomi.storage.utils.HttpsCertificateGenerator;
import it.simonedegiacomi.storage.utils.PublicIPFinder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Degiacomi Simone
 * Created  on 22/02/16.
 */
public class HttpsStorageServer {

    private final static Logger log = Logger.getLogger(HttpsStorageServer.class);

    private final Set<String> temporaryTokens = new TreeSet<>();

    private final Gson gson = new MyGsonBuilder().create();

    private StorageEnvironment env;

    private final InetSocketAddress address;

    private HttpsServer server;

    private HttpsCertificateGenerator certificateGenerator;

    public HttpsStorageServer (InetSocketAddress address, StorageEnvironment env) throws IOException, StorageException {
        this.env = env;
        this.address = address;

        // Prepare the https server
        server = HttpsServer.create(address, 0);

        try {
            // Initialize the https server with a new certificate
            certificateGenerator = new HttpsCertificateGenerator();
            server.setHttpsConfigurator(certificateGenerator.getHttpsConfigurator());

        } catch (Exception ex) {
            throw new StorageException("HTTPS certificate generation failed");
        }

        // Add the https handlers
        registerHandlers();
    }

    /**
     * Start the https server
     */
    public void serve () {
        server.start();
    }

    private void registerHandlers () {
        // Handler used to generate the token
        server.createContext("/directLogin", new DirectLoginHandler(temporaryTokens));

        // Handler that receive new file (upload from client)
        //server.createContext("/toStorage", new ToStorageHttpHandler());

        // Handler that send file to the client (download from the storage to the client)
        //server.createContext("/fromStorage", new FromStorageHttpHandler());
    }

    /**
     * Return the WS component (WSQueryHandler) that is needed for the direct connection
     * @return WSQueryHandler used to complete the direct connection handshake
     */
    public WSQueryHandler getWSComponent() {
        return new WSQueryHandler() {

            @WSQuery(name = "directLogin")
            @Override
            public JsonElement onQuery(JsonElement data) {
                // Prepare the response
                JsonObject response = new JsonObject();

                // Generate a new temporary token
                String temporaryToken = String.valueOf(new Random().nextLong());
                temporaryTokens.add(temporaryToken);
                response.addProperty("temporaryToken", temporaryToken);

                try {
                    // Send to the client the public IP
                    response.addProperty("publicIP", PublicIPFinder.find());
                } catch (IOException ex) {
                    log.warn(ex.toString());
                }

                // And the public port
                response.addProperty("port", address.getPort());

                // Finally send the public key of the https certificate
                response.add("publicKey", gson.toJsonTree(certificateGenerator.getPublicKey(), PublicKey.class));

                return response;
            }
        };
    }

    /**
     * Stop the https server
     */
    public void shutdown() {
        server.stop(0);

        temporaryTokens.clear();
    }

    /**
     * Try to forward the https port
     */
    public void forwardPort () {

    }
}