package it.simonedegiacomi.storage.direct;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.io.IOException;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class AuthMiddleware {

    private final JwtParser jwtParser;

    public AuthMiddleware (byte[] jwtSecret) {

        this.jwtParser = Jwts.parser().setSigningKey(jwtSecret);
    }

    public HttpHandler wrap (HttpHandler next) {

        return new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) throws IOException {

                String token = null;

                // Check if the auth token is in the auth header
                Headers headers = httpExchange.getRequestHeaders();

                if(headers.containsKey("Authorization")) {

                    token = headers.getFirst("Authorization").substring("Bearer ".length());
                } else {

                    // Check in the cookies
                    String cookieString = httpExchange.getRequestHeaders().getFirst("Cookie");

                    if (cookieString == null) {
                        httpExchange.sendResponseHeaders(401, 0);
                        httpExchange.close();
                        return;
                    }

                    // For each cookie
                    for(String cookie : cookieString.split(";")) {

                        if (cookie.startsWith("GoBoxDirect")) {

                            token = cookie.split("=")[1];
                            break;
                        }
                    }

                    // Cookie with the token not found
                }

                // Check the token
                if (token == null) {

                    httpExchange.sendResponseHeaders(401, 0);
                    httpExchange.close();
                    return;
                }

                // Verify the token
                try {

                    jwtParser.parse(token);
                } catch (Exception ex) {

                    httpExchange.sendResponseHeaders(401, 0);
                    httpExchange.close();
                    return;
                }

                // Ok, call the next handler
                next.handle(httpExchange);
            }
        };
    }
}
