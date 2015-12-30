package goboxclient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public class Config {
    private Properties properties;

    public Config() {
        properties = new Properties();
    }

    public static Config load () throws Exception {
        // Open the file
        FileInputStream in = new FileInputStream("gobox.conf");
        // Create a new config
        Config config = new Config();
        // Load the properties
        config.properties.load(in);
        // close the file stream
        in.close();
        // Return the loaded config
        return config;
    }

    public void save () throws Exception {
        // Create or open the file
        FileOutputStream out = new FileOutputStream("gobox.conf");
        // Save the config
        properties.store(out, "");
        // Close the stream
        out.close();
    }

    public String getProperty (String key) {
        return properties.getProperty(key);
    }

    public void setProperty (String key, String value) {
        properties.setProperty(key, value);
    }
}
