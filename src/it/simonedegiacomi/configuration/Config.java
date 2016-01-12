package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public class Config {

    public static final int CLIENT_MODE = 0;
    public static final int STORAGE_MODE = 1;

    private static final String DEFAULT_LOCATION = "config/gobox.conf";
    private static final String DEFAULT_URLS_LOCATION = "config/urls.conf";

    private static final String EXECUTION_MODE = "mode";

    private static Config ourInstance = new Config();

    private Properties properties;

    private URLBuilder urls;

    private Config () {
        properties = new Properties();
    }

    public static Config getInstance () {
        return ourInstance;
    }

    public void load () throws Exception {

        // First load the it.simonedegiacomi.configuration

        // Open the file
        FileInputStream in = new FileInputStream(DEFAULT_LOCATION);
        properties.load(in);
        in.close();

        // Now loads the url, placed in another file
        in = new FileInputStream(DEFAULT_URLS_LOCATION);

        // Create a new url builder
        urls = new URLBuilder();

        // load the urls
        urls.load(in);

        // Close the stream
        in.close();

    }

    public int getMode () {
        return Integer.parseInt(properties.getProperty(EXECUTION_MODE));
    }

    public void setMode (int mode)  {
        properties.setProperty(EXECUTION_MODE, String.valueOf(mode));
    }

    public void importLinks (InputStream in) throws IOException {
        Properties temp = new Properties();
        temp.load(in);
        in.close();
        // Copy the properties from the link to the config
        for(Object link : temp.keySet())
            properties.put(link, temp.get(link));
    }

    public void save () throws Exception {
        // Create or open the file
        FileOutputStream out = new FileOutputStream(DEFAULT_LOCATION);
        // Save the config
        properties.store(out, new String());
        // Close the stream
        out.close();
    }

    public String getProperty (String key) {
        return properties.getProperty(key);
    }

    public void setProperty (String key, String value) {
        properties.setProperty(key, value);
    }

    public URLBuilder getUrls() {
        return urls;
    }
}
