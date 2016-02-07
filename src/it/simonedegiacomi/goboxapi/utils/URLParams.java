package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This class has some static method that allows you
 * to create url with string query parameters
 *
 * Created by Degiacomi Simone onEvent 06/01/16.
 */
public class URLParams {



    /**
     * Create a new URL with the parameters form a url
     * as string and the arguments as JSONObject
     * @param stringUrl base url
     * @param params parameters to add in the url
     * @return new url with the parameters
     * @throws MalformedURLException
     */
    public static URL createURL (String stringUrl, JsonObject params) throws MalformedURLException {

        // Use a string builder to decrease the use of memory
        StringBuilder builder = new StringBuilder();

        // And add each params iterating the arguments object
        boolean first = true;
        try {
            for(Map.Entry<String, JsonElement> entry : params.entrySet()) {
                if (first) {
                    builder.append('?');
                    first = !first;
                } else
                    builder.append('&');
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                builder.append('=');
                builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name()));
            }

            return new URL(stringUrl + builder.toString());
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static URL createURL (String stringUrl, JsonObject params, boolean singleParam) throws MalformedURLException {

        try {
            if(singleParam)
                return new URL(stringUrl + "?json=" + URLEncoder.encode(params.toString(), StandardCharsets.UTF_8.name()));
            else
                return createURL(stringUrl, params);
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    /**
     * Create a new url using the url passed as argument and
     * appending the parameters
     * @param url url
     * @param params parameters as JSONObject
     * @return The new url with the parameters
     * @throws MalformedURLException
     */
    public static URL createURL (URL url, JsonObject params) throws MalformedURLException {
        return createURL(url.toString(), params);
    }
}