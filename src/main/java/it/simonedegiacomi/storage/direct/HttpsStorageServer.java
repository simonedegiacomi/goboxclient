package it.simonedegiacomi.storage.direct;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.utils.HttpsCertificateGenerator;
import it.simonedegiacomi.storage.utils.IPFinder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
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
     * Gson used by ws component that handles the direct login
     */
    private final Gson gson = new MyGsonBuilder().create();

    /**
     * Map of temporary auth token
     */
    private final Set<String> temporaryTokens = new TreeSet<>();

    /**
     * Server listen address
     */
    private final InetSocketAddress address;

    /**
     * Java https server
     */
    private HttpsServer server;

    private Certificate certificate;

    /**
     * Middleware that adds the cors headers
     */
    private final CorsHeadersMiddleware corsMiddleware = new CorsHeadersMiddleware();

    /**
     * JWT secrets to generate new direct tokens
     */
    private final byte[] jwtSecret = new byte[512];

    /**
     * Auth middleware
     */
    private final AuthMiddleware authMiddleware;

    /**
     * Create the https storage server
     * @param address Address to listen
     */
    public HttpsStorageServer (InetSocketAddress address) throws StorageException {
        this.address = address;

        // Generate a new token
        new Random().nextBytes(jwtSecret);
        authMiddleware = new AuthMiddleware(jwtSecret);

        try {
            // Create or load the https certificate
            HttpsCertificateGenerator certificateGenerator = new HttpsCertificateGenerator();
            HttpsConfigurator configurator = certificateGenerator.getHttpsConfigurator();
            certificate = certificateGenerator.getCertificate();

            // Prepare the https server
            server = HttpsServer.create(address, 0);

            // Set the https configurator
            server.setHttpsConfigurator(configurator);
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot create https server");
        }

        // Add login handler
        server.createContext("/directLogin", corsMiddleware.wrap(new DirectLoginHandler(jwtSecret, temporaryTokens)));

        // and a test handler
        server.createContext("/test", httpExchange -> {
            httpExchange.sendResponseHeaders(200, 0);
            httpExchange.getResponseBody().write("<h1>It works!</h1>".getBytes());
            httpExchange.close();
        });
    }

    public WSQueryHandler getWSQueryHandler () {
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
                    response.add("certificate", gson.toJsonTree(certificate.getEncoded(), new TypeToken<byte[]>(){}.getType()));
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
     * Start the https server listening at the specified address
     */
    public void serve () {
        log.info("Starting server");
        server.start();
        log.info("Server started");
    }

    /**
     * Stop the https server
     */
    public void shutdown() {
        if (server != null) {
            server.stop(0);
            log.info("Server stopped");
        }
        temporaryTokens.clear();
    }

    /**
     * Add a new http handler
     * @param method Method of the handled request
     * @param name Name of the request
     * @param handler Handler that will handle the request
     */
    public void addHandler (String method, String name, HttpHandler handler) {
        // TODO: restrict to the specified method
        server.createContext(name, corsMiddleware.wrap(authMiddleware.wrap(handler)));
    }
}