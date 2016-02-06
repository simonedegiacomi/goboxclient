package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * This object is used to store the url to use in
 * the program. It saves the url in  a file and reload
 * it when created. It also can create new url appending
 * string parameters to the url.
 *
 * Created by Degiacomi Simone on 09/01/16.
 */
public class URLBuilder {

    /**
     * Properties that contains the url
     */
    private final Properties properties;

    /**
     * Load the urls from a string without creating a new URLBuilder
     * @param in Stream to which read the properties
     * @throws IOException
     */
    public void load (InputStream in) throws IOException {
        properties.load(in);
    }

    /**
     * Create new empty URLBuilder
     */
    public URLBuilder() {
        this.properties = new Properties();
    }

    /**
     * Return a new url specifying the key
     * @param what The key (name of the url)
     * @return Corresponding url. If the url doesn't
     * exist a null pointer will be returned
     */
    public URL get (String what) {
        try {
            return new URL(properties.getProperty(what));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public URI getURI (String what) {
        try {
            return new URI(properties.getProperty(what));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Return a new url with the specified parameters
     * @param what Key of the url
     * @param params Parameters to append
     * @return URL with the specified parameters. If
     * the url doesn't exist, a null pointer will be
     * returned
     */
    public URL get (String what, JsonElement params) {
        try {
            return get(what, params, false);
        } catch (Exception ex) {
            return null;
        }
    }

    public URL get (String what, JsonElement params, boolean singleParam) {
        try {
            return URLParams.createURL(properties.get(what).toString(), (JsonObject) params, singleParam);
        } catch (Exception ex) {
            return null;
        }
    }
}
