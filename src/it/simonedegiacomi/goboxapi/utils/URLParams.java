package it.simonedegiacomi.goboxapi.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * This class has some static method that allows you
 * to create url with string query parameters
 *
 * Created by Degiacomi Simone on 06/01/16.
 */
public class URLParams {



    /**
     * Create a new URL with the parameters form a url
     * as string and the arguments as JSONObject
     * @param stringUrl base url
     * @param params parameters to add in the url
     * @return new url with the parameters
     * @throws MalformedURLException
     * @throws JSONException
     */
    public static URL createURL (String stringUrl, JSONObject params) throws MalformedURLException, JSONException {

        // Use a string builder to decrease the use of memory
        StringBuilder builder = new StringBuilder();

        // append the url
        builder.append(stringUrl);

        // And add each params iterating the arguments object
        boolean first = true;
        Iterator<String> it = params.keys();
        while(it.hasNext()) {
            String key = it.next();
            if (first) {
                builder.append('?');
                first = !first;
            } else
                builder.append('&');
            builder.append(key);
            builder.append('=');
            builder.append(params.get(key).toString());
        }
        try {
            return new URL(URLEncoder.encode(builder.toString(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) { }
        return null;
    }

    /**
     * Create a new url using the url passed as argument and
     * appending the parameters
     * @param url url
     * @param params parameters as JSONObject
     * @return The new url with the parameters
     * @throws MalformedURLException
     * @throws JSONException
     */
    public static URL createURL (URL url, JSONObject params) throws MalformedURLException, JSONException {
        return createURL(url.toString(), params);
    }
}