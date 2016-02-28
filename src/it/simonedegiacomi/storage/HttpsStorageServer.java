package it.simonedegiacomi.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsServer;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.storage.handlers.DirectLoginHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Degiacomi Simone
 * Created  on 22/02/16.
 */
public class HttpsStorageServer {

    private final Set<String> temporaryTokens = new TreeSet<>();

    private HttpsServer server;

    public HttpsStorageServer (InetSocketAddress address) throws IOException, NoSuchAlgorithmException {
        // Prepare the https server
        server = HttpsServer.create(address, 0);

        /**


        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Create a new keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        // Initialize it
        keyStore.load(null, null);
        // Prepare the pair of key selecting the type of algorithm
        CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        X500Name x500Name = new X500Name("address", "GoBox", "GoBox", "city", "state", "country");
        // Generate the key
        keyPair.generate(1024);
        // Get the private key
        PrivateKey privateKey = keyPair.getPrivateKey();

        X509Certificate[] chain = new X509Certificate[1];

        chain[0] = keyPair.getSelfCertificate(x500Name, new Date(), (long) 1096 * 24 * 60 * 60);

        keyStore.setKeyEntry("GoBox Direct", privateKey, "pass".toCharArray(), chain);


        // Add the handlers
        registerHandlers();

         **/
    }

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

    public WSQueryHandler getWSComponent() {
        return new WSQueryHandler() {
            @Override
            public JsonElement onQuery(JsonElement data) {
                JsonObject response = new JsonObject();

                // Generate a new temporary token
                String temporaryToken = String.valueOf(new Random().nextLong());
                // Save it
                temporaryTokens.add(temporaryToken);
                // And send it back to the client
                response.addProperty("temporaryToken", temporaryToken);

                return response;
            }
        };
    }
}