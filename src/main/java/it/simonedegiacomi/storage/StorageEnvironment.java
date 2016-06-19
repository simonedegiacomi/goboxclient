package it.simonedegiacomi.storage;

import com.j256.ormlite.support.ConnectionSource;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;

public class StorageEnvironment extends GoBoxEnvironment {

    /**
     * Database connection
     */
    private ConnectionSource dbConnection;

    /**
     * Global configuration
     */
    private final Config globalConfig = Config.getInstance();

    /**
     * Event emitter
     */
    private EventEmitter emitter;

    public StorageEnvironment (GoBoxEnvironment env) {
        super(env);
    }

    /**
     * Return the database connection
     * @return Database connection
     */
    public ConnectionSource getDbConnection() {
        return dbConnection;
    }

    /**
     * Set the database connection
     * @param dbConnection Database connection
     */
    public void setDbConnection(ConnectionSource dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Return the event emitter
     * @return Event emitter
     */
    public EventEmitter getEmitter() {
        return emitter;
    }

    /**
     * Set the event emitter
     * @param emitter Event emitter
     */
    public void setEmitter(EventEmitter emitter) {
        this.emitter = emitter;
    }


    public Config getGlobalConfig() {
        return globalConfig;
    }

}