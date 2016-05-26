package it.simonedegiacomi.storage;

import com.j256.ormlite.support.ConnectionSource;
import it.simonedegiacomi.storage.direct.HttpsStorageServer;
import it.simonedegiacomi.storage.direct.UDPStorageServer;
import it.simonedegiacomi.sync.Sync;

public class StorageEnvironment {

    private ConnectionSource dbConnection;

    private StorageDB db;

    private Storage storage;

    private UDPStorageServer udpServer;

    private HttpsStorageServer httpsServer;

    private EventEmitter emitter;

    private InternalClient internalClient;

    private Sync sync;

    /**
     * @deprecated
     */
    public StorageDB getDB() {
        return db;
    }

    public ConnectionSource getDbConnection() {
        return dbConnection;
    }

    public void setDbConnection(ConnectionSource dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * @deprecated
     */
    public void setDB(StorageDB db) {
        this.db = db;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * @deprecated
     */
    public UDPStorageServer getUdpServer() {
        return udpServer;
    }

    public void setUdpServer(UDPStorageServer udpServer) {
        this.udpServer = udpServer;
    }

    /**
     * @deprecated
     */
    public HttpsStorageServer getHttpsServer() {
        return httpsServer;
    }

    /**
     * @deprecated
     */
    public void setHttpsServer(HttpsStorageServer httpsServer) {
        this.httpsServer = httpsServer;
    }

    public EventEmitter getEmitter() {
        return emitter;
    }

    public void setEmitter(EventEmitter emitter) {
        this.emitter = emitter;
    }

    public InternalClient getInternalClient() {
        return internalClient;
    }

    public void setInternalClient(InternalClient internalClient) {
        this.internalClient = internalClient;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }
}