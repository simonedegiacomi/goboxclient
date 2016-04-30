package it.simonedegiacomi.storage.direct;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Created on 30/04/16.
 * @author Degiacomi Simone
 */
public class CorsHeadersMiddleware {

    public HttpHandler wrap (HttpHandler handler) {
        return new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                addHeader(httpExchange);
                if (httpExchange.getRequestMethod().equals("OPTIONS")) {
                    httpExchange.sendResponseHeaders(200, 0);
                    httpExchange.close();
                    return;
                }
                handler.handle(httpExchange);
            }
        };
    }

    private static void addHeader (HttpExchange httpExchange) {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "https://gobox-simonedegiacomi.c9users.io");
        httpExchange.getResponseHeaders().add("Access-Control-Request-Headers", "X-PINGOTHER, Content-Type");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "GET, POST");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        httpExchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
    }
}
