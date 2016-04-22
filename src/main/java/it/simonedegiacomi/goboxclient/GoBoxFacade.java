package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.StandardClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.Work;
import it.simonedegiacomi.utils.EasyProxy;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * This facade class is some sort of control panel of the program instance. From here you
 * can get the status of the program instance, stop it and set other settings.
 *
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxFacade {

    /**
     * Logger of the class
     */
    private static final Logger logger = Logger.getLogger(GoBoxFacade.class);

    /**
     * Configuration
     */
    private final Config config = Config.getInstance();



    /**
     * Storage of the program instance
     */
    private Storage storage;

    /**
     * Client of the program instance
     */
    private Client client;

    /**
     * Sync of the instance
     */
    private Sync sync;

    /**
     * Create a new gobox facade object, and add a listener to the config
     */
    public  GoBoxFacade () {
        config.addOnconfigChangeListener(new Config.OnConfigChangeListener() {

            @Override
            public void onChange() {
                EasyProxy.manageProxy(config);
            }
        });
    }

    /**
     * Return the storage of the program instance if running in Storage mode
     * @return Storage of the program instance
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * Set the current storage
     * @param storage Current storage of the program instance
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Return the client used in this program instance
     * @return Instance of the client used in this program
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set the client used in this program instance
     * @param client Used client
     */
    public void setClient(Client client) {
        this.client = client;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    /**
     * Shutdown the sync, the client and the storage (if was running)
     */
    public void shutdown () {
        try {
            sync.shutdown();
            client.shutdown();
            storage.shutdown();
        } catch (ClientException ex) {
            logger.warn(ex);
        } catch (InterruptedException ex) {
            logger.warn(ex);
        }
    }

    /**
     * Call shutdown and then exit the program
     */
    public void exit () {
        shutdown();
        System.exit(0);
    }

    /**
     * This method load the urls and the config
     * @throws IOException File not found or invalid
     */
    public void initializeEnvironment () throws IOException {

        // Load the urls
        config.loadUrls();

        // Set the urls to the object that used this
        Auth.setUrlBuilder(config.getUrls());
        StandardClient.setUrlBuilder(config.getUrls());

        // Load the other config (such as auth credentials)
        config.load();

        // Apply this config and reload the needed components with the new config
        config.apply();
    }

    /**
     * Return a set with the current running sync work
     * @return Set of works
     */
    public Set<Work> getRunningWorks () { return sync.getWorker().getCurrentWorks(); }

    /**
     * Start or stop syncing
     * @param syncState State of the sync object
     */
    public void setSyncing (boolean syncState) {
        try {
            sync.setSyncing(syncState);
        } catch (Exception ex) {
            // TODO: fix this
        }
    }

    public boolean isSyncing() {
        return sync.isSyncing();
    }

    public boolean isStorageConnected() {
        return storage != null || client.isReady();
    }

    public void connect() {
    }

    public boolean isStorageMode() {
        return config.isAuthDefined() ? config.getAuth().getMode().equals(Auth.Modality.STORAGE) : false;
    }
}