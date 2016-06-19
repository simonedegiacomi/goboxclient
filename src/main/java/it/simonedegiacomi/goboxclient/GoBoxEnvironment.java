package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.ui.GoBoxModel;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

/**
 * Created by simone on 17/06/16.
 */
public class GoBoxEnvironment {

    private static final Logger log = Logger.getLogger(GoBoxEnvironment.class);

    /**
     * Environment configuration
     */
    private final Config config = Config.getInstance();

    /**
     * Current used client
     */
    private GBClient client;

    /**
     * File system watcher
     */
    private MyFileSystemWatcher fileSystemWatcher;

    /**
     * Sync object
     */
    private Sync sync;

    /**
     * Model to communicate messages and update status
     */
    private GoBoxModel model;

    public GoBoxEnvironment () {}

    public GoBoxEnvironment(GoBoxEnvironment env) {
        setClient(env.getClient());
        setFileSystemWatcher(env.getFileSystemWatcher());
        setSync(env.getSync());
        setModel(env.getModel());
    }

    public GBClient getClient() {
        return client;
    }

    public void setClient(GBClient client) {
        this.client = client;
    }

    public MyFileSystemWatcher getFileSystemWatcher() {
        return fileSystemWatcher;
    }

    public void setFileSystemWatcher(MyFileSystemWatcher fileSystemWatcher) {
        this.fileSystemWatcher = fileSystemWatcher;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public GoBoxModel getModel() {
        return model;
    }

    public void setModel(GoBoxModel model) {
        this.model = model;
    }

    public GBAuth getAuth() {
        return config.getAuth();
    }

    /**
     * Shutdown every object
     */
    public void shutdown() {
        try {
            client.shutdown();
        } catch (ClientException ex) {
            log.warn(ex.toString(), ex);
        }
        try {
            sync.shutdown();
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
        }
        try {
            fileSystemWatcher.shutdown();
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
        }
    }
}