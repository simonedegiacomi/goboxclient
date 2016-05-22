package it.simonedegiacomi.storage;

import com.google.common.io.ByteStreams;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.*;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * This client is used when the client is executed on the same instance of the storage
 *
 * @author Degiacomi Simone
 * Created on 02/01/2016.
 */
public class InternalClient extends GBClient {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(InternalClient.class.getName());

    /**
     * Database of the GoBox Storage
     */
    private final StorageDB db;

    /**
     * Event emitter
     */
    private final EventEmitter emitter;

    /**
     * Create a new internal client using the environment
     * @param env Environment to use
     */
    public InternalClient (StorageEnvironment env) {
        if (env.getDB() == null)
            throw new InvalidParameterException("environment without db");

        if (env.getEmitter() == null)
            throw new InvalidParameterException("environment without emitter");

        this.db = env.getDB();
        this.emitter = env.getEmitter();
    }

    @Override
    public boolean init() throws ClientException { return true; }

    /**
     * Return the file with the path and the children. This method uses the id if available, the path otherwise
     * @param file File
     * @return Detailed file
     * @throws ClientException
     */
    @Override
    public GBFile getInfo(GBFile file) throws ClientException {

        // Assert that the file is not null
        if(file == null)
            throw new InvalidParameterException("File is null");

        try {
            return db.getFile(file, true, true);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public URL getUrl(TransferProfile.Action action, GBFile file, boolean preview) {
        // Meaningless with the internal client
        return null;
    }

    /**
     * This method doesn't do anything, because the internal client already has the file
     * @param file File to download
     * @throws ClientException
     */
    @Override
    public void getFile(GBFile file) throws ClientException {
        // Just add to the recent
        try {
            db.addToRecent(file);
        } catch (StorageException ex) {
            throw new ClientException(ex.toString());
        }
    }

    /**
     * This method just copy the file obtained calling {@link #toFile()} method to the output stream
     * @param file File to download
     * @param dst Destination stream
     * @throws ClientException
     * @throws IOException
     */
    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {
        InputStream fileStream = new FileInputStream(file.toFile());
        ByteStreams.copy(fileStream, dst);

        // Call the getFile method to register the event
        getFile(file);
    }

    /**
     * Create a new folder updating the database end sending the event to all the clients
     * @param newDir New directory to create
     * @throws ClientException
     */
    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        try {
            SyncEvent event = db.insertFile(newDir);
            emitter.emitEvent(event);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void uploadFile(GBFile file, InputStream inputStream) throws ClientException, IOException {
        try {
            // Just insert the file into the database, the file is already here
            SyncEvent event = db.insertFile(file);
            emitter.emitEvent(event);
        } catch (StorageException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void trashFile(GBFile gbFile, boolean b) throws ClientException { }

    /**
     * Remove the file from the database
     * @param file File to remove
     */
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

    /**
     * Do not use add the vent listener to the internal client. Use the storage.
     * This method doesn't do anything
     * @param syncEventListener
     */
    @Override
    public void addSyncEventListener(SyncEventListener syncEventListener) { }

    /**
     * Do nothing. See {@link #addSyncEventListener(SyncEventListener)}
     * @param syncEventListener
     */
    @Override
    public void removeSyncEventListener(SyncEventListener syncEventListener) { }

    /**
     * Return always true
     * @return true
     */
    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * Return always Ready
     * @return Ready
     */
    @Override
    public ClientState getState() {
        return ClientState.READY;
    }

    @Override
    public void shutdown() { }

    @Override
    public List<GBFile> getSharedFiles() throws ClientException { return null; }

    @Override
    public void share(GBFile gbFile, boolean b) throws ClientException { }

    /**
     * Method not implemented in internal client. Use theStandard Client implementation
     * @param gbFilter
     * @return
     * @throws ClientException
     */
    @Override
    public List<GBFile> getFilesByFilter(GBFilter gbFilter) throws ClientException {
        throw new UnsupportedOperationException("use the StandardClient implementation");
    }

    @Override
    public List<SyncEvent> getRecentFiles(long l, long l1) throws ClientException {
        throw new UnsupportedOperationException("use the StandardClient implementation");
    }

    @Override
    public List<GBFile> getTrashedFiles() throws ClientException {
        throw new UnsupportedOperationException("use the StandardClient implementation");
    }

    @Override
    public void emptyTrash() throws ClientException {
        throw new UnsupportedOperationException("use the StandardClient implementation");
    }

    @Override
    public void move (GBFile src, GBFile dst, boolean copy) throws ClientException {
        try {
            SyncEvent syncEvent = db.move(src, dst,  copy);
            emitter.emitEvent(syncEvent);
        } catch (StorageException ex) {

        }
    }
}