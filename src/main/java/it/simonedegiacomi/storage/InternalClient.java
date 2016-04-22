package it.simonedegiacomi.storage;

import com.google.common.io.ByteStreams;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.*;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * This client is used when the client is executed
 * onEvent the same instance of the storage
 *
 * @author Degiacomi Simone
 * Created on 02/01/2016.
 */
public class InternalClient extends Client {

    private static final Logger log = Logger.getLogger(InternalClient.class.getName());

    /**
     * Database of the GoBox Storage
     */
    private final DAOStorageDB db;

    /**
     * Reference to the storage used to communicate
     * new files to the other clients
     */
    private final Storage storage;

    private final EventEmitter emitter;

    public InternalClient (StorageEnvironment env) {
        this.db = env.getDB();
        this.storage = env.getStorage();
        this.emitter = env.getEmitter();
    }

    @Override
    public GBFile getInfo(GBFile file) throws ClientException {
        if(file == null)
            throw new InvalidParameterException("File is null");

        try {
            return db.getFile(file);
        } catch (StorageException ex) {

            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void getFile(GBFile file) throws ClientException {

    }

    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {

        InputStream fileStream = new FileInputStream(file.toFile());
        ByteStreams.copy(fileStream, dst);
    }

    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        // The internal client never crete a new folder
    }

    @Override
    public void uploadFile(GBFile file, InputStream stream) {
        // The file is already here
        uploadFile(file);
    }

    @Override
    public void uploadFile(GBFile file) {
        try {

            // Just insert the file into the database, the file is already here
            SyncEvent event = db.insertFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {

            log.warn(ex.toString(), ex);
        }
    }

    @Override
    public void removeFile(GBFile file) {
        try {
            // Just remove the file, it's already gone...
            SyncEvent event = db.removeFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {
        try {
            // The file is already updated...
            SyncEvent event = db.updateFile(file);
            emitter.emitEvent(event);
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
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

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public ClientState getState() {
        return ClientState.READY;
    }


    @Override
    public void init() throws ClientException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<GBFile> getSharedFiles() throws ClientException {
        return null;
    }

    @Override
    public List<GBFile> getFilesByFilter(GBFilter gbFilter) throws ClientException {
        return null;
    }

    @Override
    public List<GBFile> getRecentFiles(long l, long l1) throws ClientException {
        return null;
    }

    @Override
    public List<GBFile> getTrashedFiles(long l, long l1) throws ClientException {
        return null;
    }
}
