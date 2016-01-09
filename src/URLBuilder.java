package goboxapi;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by simone on 09/01/16.
 */
public class URLBuilder {

    private final Properties properties;

    public static URLBuilder load (File file) throws IOException {
        return load(new FileInputStream(file));
    }

    public static URLBuilder load (InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);
        return properties;
    }

    public URLBuilder() {
        this.properties = new Properties();
    }

    public URL get (String what) {
        try {
            return new URL(properties.getProperty(what));
        } catch (MalformedURLException ex) {
            return null;
        }
    }
}
