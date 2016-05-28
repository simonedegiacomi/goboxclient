package it.simonedegiacomi.storage.direct;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.log4j.Logger;

/**
 * Created on 30/04/16.
 * @author Degiacomi Simone
 */
public class CorsHeadersMiddleware {

    private final Logger log = Logger.getLogger(CorsHeadersMiddleware.class);

    public HttpHandler wrap (HttpHandler handler) {
        return httpExchange -> {
            log.info("Adding cors headers");
            addHeader(httpExchange);
            if (httpExchange.getRequestMethod().equals("OPTIONS")) {
                httpExchange.sendResponseHeaders(200, 0);
                httpExchange.close();
                return;
            }
            handler.handle(httpExchange);
        };
    }

    private static void addHeader (HttpExchange httpExchange) {

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "https://gobox-simonedegiacomi.c9users.io");

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "GET, POST, PUT, OPTIONS");

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");

        httpExchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
    }
}
