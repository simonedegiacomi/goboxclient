package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created on 27/12/2015.
 *
 * @author Degiacomi Simone
 */
public class Config {

    /**
     * Logger of the class
     */
    private final static Logger log = Logger.getLogger(Config.class);

    /**
     * Default configuration filename
     */
    public static final String DEFAULT_CONFIG_FILE = "config/gobox.properties";

    /**
     * Singleton instance of the config
     */
    private static Config ourInstance = new Config();

    /**
     * Java properties, used as container for the properties
     */
    private final Properties properties = new Properties();

    /**
     * Auth object serialized in the configuration. This is final to let you set new token lister one time
     */
    private final GBAuth auth = new GBAuth();

    /**
     * Collection of listener that are called when the apply method is called
     */
    private Set<OnConfigChangeListener> listeners = new HashSet<>();

    /**
     * Private constructor to make this class a singleton
     */
    private Config() {

        // Add a listener to the auth object
        auth.addOnTokenChangeListener(() -> {
            try {
                save();
            } catch (IOException ex) {
                log.warn(ex.toString(), ex);
            }
        });
    }

    /**
     * Return the singleton instance of the config
     *
     * @return Unique configuration object
     */
    public static Config getInstance() {
        return ourInstance;
    }

    /**
     * Load from file the configuration
     *
     * @param File file with the configuration
     * @throws Exception
     */
    public void load(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        properties.load(in);
        in.close();

        // If the log configuration file is present, load it
        if (properties.containsKey("logConfiguration")) {
            PropertyConfigurator.configure(properties.getProperty("logConfiguration"));
        }

        // If the auth object was serialized, reload it
        if (properties.containsKey("username")) {
            auth.setUsername(properties.getProperty("username"));
            auth.setMode(GBAuth.Modality.valueOf(properties.getProperty("mode")));
            auth.setToken(properties.getProperty("token"));
        }
    }

    /**
     * Load the configuration from the default file. This method is just an alias for {@link #load(File)}
     *
     * @throws Exception
     */
    public void load() throws IOException {
        load(new File(DEFAULT_CONFIG_FILE));
    }

    /**
     * Save the configuration to the file and call the apply method
     *
     * @param File to which save the configuration
     * @throws IOException
     */
    public void save(File file) throws IOException {

        // If the file doesn't exists, assert that the father exists
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        // Update the properties with the auth
        if (auth.getToken() != null) {
            properties.setProperty("token", auth.getToken());
            properties.setProperty("username", auth.getUsername());
            properties.setProperty("mode", auth.getMode().toString());
        }

        // Write to the file
        FileOutputStream out = new FileOutputStream(file);
        properties.store(out, new String());
        out.close();

        apply();
    }

    /**
     * This method is just an alias for {@link #save(File)}
     */
    public void save() throws IOException {
        save(new File(DEFAULT_CONFIG_FILE));
    }

    /**
     * Call this method when you change some properties and you want to be sure that all the object
     * depending from them will receive the chamges
     */
    public void apply() {
        for (OnConfigChangeListener listener : listeners)
            listener.onChange();
    }

    /**
     * Return the request property value.
     * @param key Name of the property
     * @param defaultValue Default value if the property is not found
     * @return Property value or default property value
     */
    public String getProperty (String key, String defaultValue) {
        if(!properties.containsKey(key)) {
            log.info("Property " + key + " not found, using default value: " + defaultValue);
            properties.put(key, defaultValue);
        }

        return properties.getProperty(key);
    }

    /**
     * Return the directory for the specified purpose. If the directory doesn't exist is created and if he key doesn't
     * exist the specified default value is returned
     * @param forWhat Purpose of the folder
     * @param defaultValue Default value to use of the folder is not specified
     * @return Existing folder
     */
    public File getFolder (String forWhat, String defaultValue) {

        // Get from properties
        File folder = new File(getProperty(forWhat, defaultValue));

        if (!folder.exists()) {

            // Create the directory
            folder.mkdirs();
            log.info("Created folder " + folder.toString());
        }

        return folder;
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void addOnconfigChangeListener(OnConfigChangeListener listener) {
        listeners.add(listener);
    }

    public void deleteAuth() {
        properties.remove("username");
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public interface OnConfigChangeListener {
        public void onChange();
    }

    public GBAuth getAuth() {
        return auth;
    }

    public boolean isAuthDefined() {
        return properties.containsKey("username") && properties.containsKey("token") && properties.containsKey("mode");
    }

    /**
     * Update the internal auth object with the given one
     *
     * @param auth Object ot use to update the internal auth
     */
    public void setAuth(GBAuth auth) {
        this.auth.setUsername(auth.getUsername());
        this.auth.setMode(auth.getMode());
        this.auth.setToken(auth.getToken());
    }
}