package it.simonedegiacomi.storage;

import com.google.common.io.ByteStreams;
import it.simonedegiacomi.goboxapi.GBCache;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This client is used when the client is executed
 * onEvent the same instance of the storage
 *
 * @author Degiacomi Simone
 * Created on 02/01/2016.
 */
public class InternalClient implements Client {

    private static final Logger log = Logger.getLogger(InternalClient.class.getName());

    /**
     * Database of the GoBox Storage
     */
    private final StorageDB db;

    /**
     * Reference to the storage used to communicate
     * new files to the other clients
     */
    private final Storage storage;

    private final EventEmitter emitter;

    private final Set<String> filesToIgnore = new HashSet<>();

    public InternalClient(Storage storage, StorageDB db) {
        this.db = db;
        this.storage = storage;
        this.emitter = storage.getEventEmitter();
    }


    /**
     * Return the state of the connection with the storage. In this
     * case this will return true, because the storage is the same
     * instance
     * @return state of the connection, true
     */
    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public GBFile getInfo(GBFile file) throws ClientException {
        if(file.getID() == GBFile.UNKNOWN_ID)
            db.findIDByPath(file);
        try {
            return db.getFileById(file.getID(), true, true);
        } catch (StorageException ex) {
            //ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void getFile(GBFile file) throws ClientException {
        if(shouldIgnore(file))
            return;
    }

    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {
        if(shouldIgnore(file))
            return;
        InputStream fileStream = new FileInputStream(file.toFile());
        ByteStreams.copy(fileStream, dst);
    }

    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        this.uploadFile(newDir);
    }

    @Override
    public void uploadFile(GBFile file, InputStream stream) {
        if(shouldIgnore(file))
            return;
    }

    @Override
    public void uploadFile(GBFile file) {
        if(shouldIgnore(file))
            return;
        try {
            // Just insert the file into the database, the file is already here
            SyncEvent event = db.insertFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void removeFile(GBFile file) {
        if(shouldIgnore(file))
            return;
        try {
            // Just remove the file, it's already gone...
            SyncEvent event = db.removeFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {
        if(shouldIgnore(file))
            return;
        try {
            // The file is already updated...
            SyncEvent event = db.updateFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @Override
    public void updateFile(GBFile file) {
        this.updateFile(file, null);
    }

    @Override
    public void setSyncEventListener (SyncEventListener listener) {
        // Nothing to do here because alla the events are handled by the storage
    }

    @Override
    public void requestEvents(long lastHeardId) {
        // Not implemented yet
    }

    public void ignore(GBFile file) {
        filesToIgnore.add(file.getPathAsString());
    }

    private boolean shouldIgnore (GBFile file) {
        return filesToIgnore.remove(file.getPathAsString());
    }
}
