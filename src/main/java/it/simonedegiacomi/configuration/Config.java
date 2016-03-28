package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author Degiacomi Simone
 * Created on 27/12/2015.
 */
public class Config {

    private static final String DEFAULT_LOCATION = "config/gobox.conf";

    /**
     * Singleton instance of the config
     */
    private static Config ourInstance = new Config();

    /**
     * Java properties, used as container for the properties
     */
    private Properties properties = new Properties();

    /**
     * Container for the url
     */
    private URLBuilder urls = new URLBuilder();

    private Auth auth = new Auth();

    /**
     * Collection of listener that are called when the apply method
     * is called
     */
    private List<OnConfigChangeListener> listeners = new LinkedList<>();

    /**
     * Private constructor to make this class a singleton
     */
    private Config () { }

    /**
     * Return the singleton instance of the config
     * @return Unique configuration object
     */
    public static Config getInstance () {
        return ourInstance;
    }

    /**
     * Load from file the configuration
     * @param File file with the configuration
     * @throws Exception
     */
    public void load (File file) throws IOException {
        // Open the file
        FileInputStream in = new FileInputStream(file);
        properties.load(in);
        in.close();

        // Close the stream
        in.close();

        if(properties.getProperty("token") != null) {
            auth.setUsername(properties.getProperty("username"));
            auth.setToken(properties.getProperty("token"));
            auth.setMode(Auth.Modality.valueOf(properties.getProperty("mode")));
        }
    }

    /**
     * Load the configuration from the default file
     * @throws Exception
     */
    public void load () throws IOException {
        try {
            load(new File(DEFAULT_LOCATION));
        } catch (IOException ex) {
            save();
        }
    }

    /**
     *
     * @param file
     * @throws IOException
     */
    public void loadUrls (InputStream in) throws IOException {
        // load the urls
        urls.load(in);
    }

    /**
     *
     * @throws IOException
     */
    public void loadUrls () throws IOException {
        urls.load();
    }

    /**
     * Save the configuration to the file and call the apply method
     * @param File
     * @throws Exception
     */
    public void save (File file) throws IOException {

        // update the properties with the auth
        if(auth != null) {
            properties.setProperty("token", auth.getToken());
            properties.setProperty("username", auth.getUsername());
            properties.setProperty("mode", auth.getMode().toString());
        }

        // Create or open the file
        FileOutputStream out = new FileOutputStream(file);
        // Save the config
        properties.store(out, new String());
        // Close the stream
        out.close();

        // Execute all the listener
        apply();
    }

    /**
     * Save the configuration to the default file and
     * then call the apply method
     */
    public void save () throws IOException {
        save(new File(DEFAULT_LOCATION));
    }

    public void apply () {
        for (OnConfigChangeListener listener : listeners)
            listener.onChange();
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

    public void addOnconfigChangeListener (OnConfigChangeListener listener) {
        listeners.add(listener);
    }

    public void deleteAuth() {
        properties.remove("username");
    }

    public interface OnConfigChangeListener {
        public void onChange ();
    }

    public Auth getAuth () {
        return auth;
    }

    public void setAuth (Auth auth) {
        this.auth = auth;
    }
}
