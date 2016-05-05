package it.simonedegiacomi.utils;

import java.net.URI;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class MyHttpExchangeUtils {

    public static Map<String, String> getQueryParams (URI uri) {

        // Create the result hash map
        HashMap<String, String> headers = new HashMap<>();

        // Get the query string
        String query = uri.getQuery();

        // assert that the url contains a query
        if (query == null || query.length() <= 0)
            throw new InvalidParameterException("The url doesn't contain any query parameter");

        // Fill the map
        for (String pair : query.split("&")) {

            // Split the key and the value
            String[] pieces = pair.split("=");

            // Insert in the map
            headers.put(pieces[0], pieces[1]);
        }

        return headers;
    }
}
