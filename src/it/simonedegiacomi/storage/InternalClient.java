package it.simonedegiacomi.storage;

import com.google.common.io.ByteStreams;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This client is used when the client is executed
 * on the same instance of the it.simonedegiacomi.storage
 *
 * Created by Degiacomi Simone on 02/01/2016.
 */
public class InternalClient implements Client {

    private static final Logger log = Logger.getLogger(InternalClient.class.getName());

    /**
     * Database of the GoBox Storage
     */
    private final StorageDB db;

    /**
     * Reference to the it.simonedegiacomi.storage used to comunicate
     * new files to the other clients
     */
    private final Storage storage;

    public InternalClient(Storage storage, StorageDB db) {
        this.db = db;
        this.storage = storage;
    }


    /**
     * Return the state of the connection with the it.simonedegiacomi.storage. In this
     * case this will return true, because the it.simonedegiacomi.storage is the same
     * instance
     * @return state of the connection, true
     */
    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public GBFile[] listDirectory(GBFile father) throws ClientException {
        try {
            return db.getChildrenByFather(father);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            return null;
        }
    }

    @Override
    public void getFile(GBFile file) throws ClientException {
        return ;
    }

    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {
        InputStream fileStream = new FileInputStream(file.toFile());
        ByteStreams.copy(fileStream, dst);
    }

    @Override
    public void createDirectory(GBFile newDir) throws ClientException {

    }

    @Override
    public void uploadFile(GBFile file, InputStream stream) {

    }

    @Override
    public void uploadFile(GBFile file) {
        try {
            SyncEvent event = db.insertFile(file);
            storage.emitEvent(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void removeFile(GBFile file) {
        try {
            db.removeFile(file);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {
        uploadFile(file);
    }

    @Override
    public void updateFile(GBFile file) {
        try {
            SyncEvent event = db.updateFile(file);
            storage.emitEvent(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void setSyncEventListener (SyncEventListener listener) {
        storage.setInternalSyncEventListener(listener);
    }

}
