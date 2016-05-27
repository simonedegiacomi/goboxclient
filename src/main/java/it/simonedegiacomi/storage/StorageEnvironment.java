package it.simonedegiacomi.storage;

import com.j256.ormlite.support.ConnectionSource;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;

public class StorageEnvironment {

    /**
     * Database connection
     */
    private ConnectionSource dbConnection;

    /**
     * Global configuration
     */
    private final Config globalConfig = Config.getInstance();

    /**
     * @deprecated
     */
    private StorageDB db;

    /**
     * Event emitter
     */
    private EventEmitter emitter;

    /**
     * Internal client
     */
    private InternalClient internalClient;

    /**
     * Sync object
     * @deprecated
     */
    private Sync sync;

    /**
     * File system watcher
     */
    private MyFileSystemWatcher fileSystemWatcher;

    /**
     * @deprecated
     */
    public StorageDB getDB() {
        return db;
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
     * @deprecated
     */
    public void setDB(StorageDB db) {
        this.db = db;
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

    /**
     * Return the internal client
     * @return Internal client
     */
    public InternalClient getInternalClient() {
        return internalClient;
    }

    /**
     * Set the internal client
     * @param internalClient Internal client
     */
    public void setInternalClient(InternalClient internalClient) {
        this.internalClient = internalClient;
    }

    /**
     * Return the sync object
     * @return Sync object
     */
    public Sync getSync() {
        return sync;
    }

    /**
     * Set the sync object
     * @param sync sync object
     */
    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public Config getGlobalConfig() {
        return globalConfig;
    }

    public MyFileSystemWatcher getFileSystemWatcher() {
        return fileSystemWatcher;
    }

    public void setFileSystemWatcher(MyFileSystemWatcher fileSystemWatcher) {
        this.fileSystemWatcher = fileSystemWatcher;
    }
}