package it.simonedegiacomi.storage.handlers.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Set;

/**
 * Created by simone on 25/02/16.
 */
public class DirectLoginHandler implements HttpHandler {

    private JsonParser parser = new JsonParser();

    private Set<String> temporaryTokens;

    private byte[] jwtSecret = new byte[256];

    public DirectLoginHandler(Set<String> temporaryTokens) {
        new Random().nextBytes(jwtSecret);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // Read the json object from the request
        JsonObject request = parser.parse(new InputStreamReader(httpExchange.getRequestBody())).getAsJsonObject();
        JsonObject response = new JsonObject();

        // Check if the temporary token exists
        String temporaryToken = request.get("temporaryToken").getAsString();
        if(!temporaryTokens.remove(temporaryToken)) {
            // If it doesn't, the client is not authorized
            httpExchange.sendResponseHeaders(401, 0);
            httpExchange.close();
            return;
        }

        // Create a new jwt token
        String jwt = Jwts.builder().signWith(SignatureAlgorithm.HS256, jwtSecret).compact();

        // Send the new token to the client how it prefers
        boolean cookie = request.get("cookie").getAsBoolean();
        if(cookie) {
            // Set the token as cookie
            httpExchange.getResponseHeaders().add("Set-Cookie", jwt);
        } else {
            // Or send it in a json
            response.addProperty("token", jwt);
        }

        // Send the response
        response.addProperty("success", true);
        httpExchange.getResponseBody().write(response.toString().getBytes());
        httpExchange.close();
    }
}