package it.simonedegiacomi.storage.direct;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.handlers.http.*;
import it.simonedegiacomi.storage.utils.HttpsCertificateGenerator;
import it.simonedegiacomi.storage.utils.IPFinder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateEncodingException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created on 22/02/16.
 * @author Degiacomi Simone
 */
public class HttpsStorageServer {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(HttpsStorageServer.class);

    /**
     * Map of temporary auth token
     */
    private final Set<String> temporaryTokens = new TreeSet<>();

    private final Gson gson = new MyGsonBuilder().create();

    private final StorageEnvironment env;

    private final InetSocketAddress address;

    private HttpsCertificateGenerator certificateGenerator;

    /**
     * Java https server
     */
    private HttpsServer server;

    /**
     * Create the https storage server
     * @param address Address to listen
     * @param env Environment to use
     */
    public HttpsStorageServer (InetSocketAddress address, StorageEnvironment env) {
        this.env = env;
        this.address = address;
    }

    /**
     * Initialize the https server. This method must be called before the serve method.
     * This method try rto lock the socket address, so if the address is already in use, this
     * method will throw that specified exception
     * @throws IOException
     * @throws StorageException
     */
    public void init () throws IOException, StorageException {
        try {
            // create or load the https certificate
            certificateGenerator = new HttpsCertificateGenerator();
            HttpsConfigurator configurator = certificateGenerator.getHttpsConfigurator();

            // Prepare the https server
            server = HttpsServer.create(address, 0);

            // Set the https configurator
            server.setHttpsConfigurator(configurator);

        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("HTTPS certificate generation failed");
        }

        // Add the https handlers
        registerHandlers();
    }

    /**
     * Start the https server listening at the specified address
     */
    public void serve () {
        server.start();
    }

    /**
     * Register the http handlers
     */
    private void registerHandlers () {

        CorsHeadersMiddleware corsMiddle = new CorsHeadersMiddleware();

        // Handler for test
        server.createContext("/test", corsMiddle.wrap(new TestHandler()));

        // Handler used to generate the token
        DirectLoginHandler directLoginHandler = new DirectLoginHandler(temporaryTokens);
        server.createContext("/directLogin", corsMiddle.wrap(directLoginHandler));

        // Crate the auth middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(directLoginHandler.getJwtSecret());

        // Handler that receive new file (upload from client)
        server.createContext("/toStorage", corsMiddle.wrap(authMiddleware.wrap(new ToStorageHttpHandler(env))));

        // Handler that send file to the client (download from the storage to the client)
        server.createContext("/fromStorage", corsMiddle.wrap(authMiddleware.wrap(new FromStorageHttpHandler(env.getDB()))));
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
                    response.addProperty("publicIP", IPFinder.findPublic());

                    // And also the probable local ip
                    response.addProperty("localIP", IPFinder.findProbableLocal());

                    // And the public port
                    response.addProperty("port", address.getPort());

                    // Finally send the public key of the https certificate
                    response.add("certificate", gson.toJsonTree(certificateGenerator.getCertificate().getEncoded(), new TypeToken<byte[]>(){}.getType()));
                } catch (IOException ex) {
                    log.warn(ex.toString(), ex);
                    response.addProperty("error", true);
                } catch (CertificateEncodingException ex) {
                    log.warn(ex.toString(), ex);
                    response.addProperty("error", true);
                }

                return response;
            }
        };
    }

    /**
     * Stop the https server
     */
    public void shutdown() {
        if (server != null)
            server.stop(0);
        temporaryTokens.clear();
    }

    public void addHandler (String method, String name, HttpHandler handler) {
        server.createContext(name, handler);
    }
}