package it.simonedegiacomi.sync;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import net.contentobjects.jnotify.JNotifyException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * A Sync object work with an implementation of the Client interface, and manage
 * the synchronization of the filesystem with the relative storage of the account.
 *
 * Created on 24/12/2015
 * @author Degiacomi Simone
 */
public class Sync {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(Sync.class.getName());

    /**
     * Worker of the class. This worker is used to keep a simple queue for
     * thw works (uploads and downloads)
     */
    private final Worker worker;

    /**
     * Client object used as API interface to communicate with the storage
     */
    private Client client;

    /**
     * Configuration of the environment
     */
    private static final Config config = Config.getInstance();

    /**
     * The FileSystemWatcher is the object that pool and watch the local fileSystem,
     * notifying any creation, changes or deletion of a file (or folder)
     */
    private FileSystemWatcher watcher;

    /**
     * Path of the files folder
     */
    private static final String PATH = config.getProperty("path");

    private volatile boolean syncState;

    /**
     * Create and start keep in sync the local fs with the GoBox Storage. It used the
     * Client passed as arguments to communicate the events and to get the changes from
     * the storage
     * @throws IOException Exception thrown assigning the file system watcher
     */
    public Sync (Client client) throws IOException {

        this.client = client;

        // Create a new work
        worker = new Worker(client, Worker.DEFAULT_THREADS);
        Work.setWorker(worker);

        // Create the file system watcher
        createWatcher();
    }

    /**
     * create the file system watcher
     * @throws IOException
     */
    private void createWatcher () throws IOException {
        watcher = new FileSystemWatcher(PATH, new FileSystemWatcher.FileSystemEventListener() {

            @Override
            public void onFileCreated(File newFile) {

                // Wrap the java File into a GoBoxFile
                GBFile wrappedFile = new GBFile(newFile, PATH);

                // Add the work
                worker.addWork(new Work(wrappedFile, Work.WorkKind.UPLOAD));
            }

            @Override
            public void onFileModified(File modifiedFile) {

                // Wrap the java File into a GoBoxFile
                GBFile wrappedFile = new GBFile(modifiedFile, PATH);

                // Create the new work
                worker.addWork(new Work(wrappedFile, Work.WorkKind.UPLOAD));
            }

            @Override
            public void onFileDeleted(File deletedFile) {

                // Wrap the file
                GBFile wrappedFile = new GBFile(deletedFile, PATH);

                // Create the work
                worker.addWork(new Work(wrappedFile, Work.WorkKind.REMOVE_IN_STORAGE));
            }

            @Override
            public void onFileMoved(File before, File movedFile) {

                // Wrap the file
                GBFile wrappedFile = new GBFile(movedFile, PATH);

                // Create the work
                worker.addWork(new Work(wrappedFile, Work.WorkKind.MOVE_IN_STORAGE));
            }
        });
    }

    /**
     * Sync the file system with the storage after a period of sleep. Then start
     * the file system watcher and listen for events from the client
     * @throws IOException
     * @throws ClientException If there is some problem with the client class
     */
    public void syncAndStart() throws IOException, ClientException {
        log.info("Start synchronization with storage");

        // Change sync flag
        syncState = true;

        // Check the root and all the subfolder
        checkR(GBFile.ROOT_FILE);
        log.info("ReSync completed");

        // And listen for event's from the storage
        assignSyncEventFromStorage();
    }

    /**
     * Check the file passed as argument recursively
     * @param file File to compare with the storage
     * @throws IOException
     * @throws ClientException
     */
    private void checkR (GBFile file) throws IOException, ClientException {

        // Get details about this file
        GBFile detailedFile = client.getInfo(file);

        // If the storage doesn't know anything about this file
        if(detailedFile == null) {

            // Upload it
            worker.addWork(new Work(file, Work.WorkKind.UPLOAD));
            return;
        }

        detailedFile.setPrefix(PATH);

        // Check if i have this file
        if (!detailedFile.toFile().exists()) {
            worker.addWork(new Work(file, Work.WorkKind.DOWNLOAD));
            return;
        }

        MyFileUtils.loadFileAttributes(detailedFile);

        // If it's a directory
        if(detailedFile.isDirectory()) {

            // Create a map with the name of the file as key and the GBFile
            // as value. These files are the children of the folder
            Map<String, GBFile> storageFiles = new HashMap<>();
            for (GBFile child : detailedFile.getChildren())
                storageFiles.put(child.getName(), child);

            // Check every children (in the fs)
            for (File child : detailedFile.toFile().listFiles()) {

                // Check
                checkR(new GBFile(child, PATH));

                // And then remove from the map
                storageFiles.remove(child.getName());
            }

            // Wait! and the remaining files in the map?
            // This client doesn't have these file!
            for (Map.Entry<String, GBFile> entry : storageFiles.entrySet()) {

                // Download it
                worker.addWork(new Work(entry.getValue(), Work.WorkKind.DOWNLOAD));
            }

            // Empty the map
            storageFiles.clear();
        } else {

            if (detailedFile.getLastUpdateDate() == file.getLastUpdateDate()) {
                // Already up to date
                return;
            }

            // If it's not a directory but it's a file check who have the latest version
            Work.WorkKind action = detailedFile.getLastUpdateDate() > file.getLastUpdateDate() ? Work.WorkKind.UPLOAD : Work.WorkKind.DOWNLOAD;
            worker.addWork(new Work(detailedFile, action));
        }
    }

    /**
     * This method set the listener of the client object
     * that will listen at the events transmitted from
     * the storage. These events are the result of the
     * operation onEvent the files from other clients.
     */
    private void assignSyncEventFromStorage () {

        // Set the listener
        client.addSyncEventListener(new SyncEventListener() {

            @Override
            public void on(SyncEvent event) {

                // Get the GBFile of this event
                GBFile file = event.getRelativeFile();
                file.setPrefix(PATH);

                GBFile before = event.getBefore();
                if (before != null)
                    before.setPrefix(PATH);

                // Queue work
                worker.addWork(new Work(event));
            }
        });
    }

    /**
     * Stop the sync object.
     * @throws InterruptedException
     */
    public void shutdown () throws InterruptedException {
        syncState = false;
        try {
            watcher.shutdown();
            worker.shutdown();
        } catch (JNotifyException ex) {
            log.warn(ex.toString(), ex);
        }
    }

    public FileSystemWatcher getFileSystemWatcher () {
        return watcher;
    }

    public Worker getWorker () { return worker; }

    public void setSyncing(boolean newState) throws InterruptedException, ClientException, IOException {
        if (newState == syncState)
            return;
        if (newState)
            syncAndStart();
        else
            shutdown();
    }

    public boolean isSyncing() {
        return syncState;
    }
}