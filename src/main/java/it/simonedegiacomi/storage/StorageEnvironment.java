package it.simonedegiacomi.storage;

import it.simonedegiacomi.storage.direct.HttpsStorageServer;
import it.simonedegiacomi.storage.direct.UDPStorageServer;
import it.simonedegiacomi.sync.Sync;

public class StorageEnvironment {

    private StorageDB db;

    private Storage storage;

    private UDPStorageServer udpServer;

    private HttpsStorageServer httpsServer;

    private EventEmitter emitter;

    private InternalClient internalClient;

    private Sync sync;

    public StorageDB getDB() {
        return db;
    }

    public void setDB(StorageDB db) {
        this.db = db;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public UDPStorageServer getUdpServer() {
        return udpServer;
    }

    public void setUdpServer(UDPStorageServer udpServer) {
        this.udpServer = udpServer;
    }

    public HttpsStorageServer getHttpsServer() {
        return httpsServer;
    }

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