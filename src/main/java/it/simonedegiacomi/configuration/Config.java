package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
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
    private static final String DEFAULT_LOCATION = "config/gobox.properties";

    /**
     * Default logger configuration file name
     */
    private static final String DEFAULT_LOG_CONFIG = "config/log.conf";

    /**
     * Singleton instance of the config
     */
    private static Config ourInstance = new Config();

    /**
     * Java properties, used as container for the properties
     */
    private final Properties properties = new Properties();

    /**
     * Container for the url
     */
    private final URLBuilder urls = new URLBuilder();

    /**
     * Auth object serialized in the configuration. This is final to let you set new token lister one time
     */
    private final Auth auth = new Auth();

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
                log.info("config saved");
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
     * Load the default logger config
     */
    public static void loadLoggerConfig() {
        PropertyConfigurator.configure(DEFAULT_LOG_CONFIG);
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

        // If the auth object was serialized, reload it
        if (properties.containsKey("username")) {
            auth.setUsername(properties.getProperty("username"));
            auth.setToken(properties.getProperty("token"));
            auth.setMode(Auth.Modality.valueOf(properties.getProperty("mode")));
        }
    }

    /**
     * Load the configuration from the default file. This method is just an alias for {@link #load(File)}
     *
     * @throws Exception
     */
    public void load() throws IOException {
        load(new File(DEFAULT_LOCATION));
    }

    /**
     * Load urls
     *
     * @throws IOException
     */
    public void loadUrls() throws IOException {
        urls.load();
    }

    /**
     * Save the configuration to the file and call the apply method
     *
     * @param File to which save the configuration
     * @throws IOException
     */
    public void save(File file) throws IOException {

        // Update the properties with the auth
        properties.setProperty("token", auth.getToken());
        properties.setProperty("username", auth.getUsername());
        if (auth.getMode() != null)
            properties.setProperty("mode", auth.getMode().toString());

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
        save(new File(DEFAULT_LOCATION));
    }

    /**
     * Call this method when you change some properties and you want to be sure that all the object
     * depending from them will receive the chamges
     */
    public void apply() {
        for (OnConfigChangeListener listener : listeners)
            listener.onChange();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public URLBuilder getUrls() {
        return urls;
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

    public interface OnConfigChangeListener {
        public void onChange();
    }

    public Auth getAuth() {
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
    public void setAuth(Auth auth) {
        this.auth.setUsername(auth.getUsername());
        this.auth.setToken(auth.getToken());
        this.auth.setMode(auth.getMode());
    }
}