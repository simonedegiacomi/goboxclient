package it.simonedegiacomi.goboxapi.utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
     * Load the urls from a file.
     * @param file File to read
     * @return New instance of URLBuilder with
     * the loaded urls.
     * @throws IOException
     */
    public static URLBuilder load (File file) throws IOException {
        return load(new FileInputStream(file));
    }

    /**
     * Load the urls from an input stream.
     * @param in Input stream to read
     * @return New instance of URLBuilder with loaded
     * urls.
     * @throws IOException
     */
    public static URLBuilder load (InputStream in) throws IOException {
        URLBuilder url = new URLBuilder();
        url.properties.load(in);
        return url;
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
     * @return Corrisponding url. If the url doesn't
     * exist a null pointer will be returned
     */
    public URL get (String what) {
        try {
            return new URL(properties.getProperty(what));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * Return a new url with the specified parameters
     * @param what Key of the url
     * @param params Parameters to append
     * @return URL with the specified parameters. If
     * the url dioesn't exist, a null pointer will be
     * returned
     */
    public URL get (String what, JSONObject params) {
        try {
            return URLParams.createURL(properties.get(what).toString(), params);
        } catch (Exception ex) {
            return null;
        }
    }
}
