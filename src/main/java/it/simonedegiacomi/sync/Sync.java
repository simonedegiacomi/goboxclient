package it.simonedegiacomi.sync;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static it.simonedegiacomi.goboxapi.GBFile.ROOT_ID;

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

        worker = new Worker(client, Worker.DEFAULT_THREADS);

        // Create the new watcher for the fileSystem
        Path pathToWatch = new File(PATH).toPath();
        watcher = new FileSystemWatcher(pathToWatch);

        // Set the watcher in the work class
        Work.setWatcher(watcher);

        // Assign the event of the file watcher
        assignFileWatcherEvents();
    }

    public void setClient (Client client) {

        this.client = client;
    }

    /**
     * Sync the file system with the storage after a period of sleep. Then start
     * the file system watcher and listen for events from the client
     * @throws IOException
     * @throws ClientException If there is some problem with the client class
     */
    public void resyncAndStart () throws IOException, ClientException {
        syncState = true;

        checkR(GBFile.ROOT_FILE);

        // Start watching for changes
        watcher.start();

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

            // Empty the mash
            storageFiles.clear();
        } else {

            // If it's not a directory but it's a file check who have the latest version
            Work.WorkKind action = detailedFile.getLastUpdateDate() > file.getLastUpdateDate() ? Work.WorkKind.UPLOAD : Work.WorkKind.DOWNLOAD;
            worker.addWork(new Work(detailedFile, action));
        }
    }

    /**
     * This method assign action for the possible events of
     * the fileSystemWatcher. For each event the relative
     * method of the client ( the object that wraps the API of
     * the storage) will be called.
     */
    private void assignFileWatcherEvents () {
        // At the beginning i download the file list, and make a control.
        // I download the file from the server, and new files found are transmitted to server.
        // If a file was deleted onEvent the client with the program not running, the file will
        // redownloaded

        // This event is called when a new file or directory is created
        watcher.addListener(FileSystemWatcher.FILE_CREATED, new FileSystemWatcher.Listener() {

            @Override
            public void onEvent(File newFile) {

                // Wrap the java File into a GoBoxFile
                GBFile wrappedFile = new GBFile(newFile, PATH);

                // Add the work
                worker.addWork(new Work(wrappedFile, Work.WorkKind.UPLOAD));
            }
        });

        // Event thrown when the file is edited
        watcher.addListener(FileSystemWatcher.FILE_CHANGED, new FileSystemWatcher.Listener() {

            @Override
            public void onEvent(File editedFile) {

                try {

                    // Wrap the java File into a GoBoxFile
                    GBFile wrappedFile = new GBFile(editedFile, PATH);

                    // Call the right client method
                    client.uploadFile(wrappedFile);
                } catch (ClientException ex) {

                    log.warn(ex.toString(), ex);
                } catch (IOException ex) {

                    ex.printStackTrace();
                }
            }
        });

        // Event called when a file is removed
        watcher.addListener(FileSystemWatcher.FILE_DELETED, new FileSystemWatcher.Listener() {

            @Override
            public void onEvent(File deletedFile) {
                log.info("A file in the local fs was deleted");

                try {

                    // Wrap the file
                    GBFile wrappedFile = new GBFile(deletedFile, PATH);

                    // and remove it
                    client.removeFile(wrappedFile);
                } catch (ClientException ex) {

                    log.warn(ex.toString(), ex);
                }
            }
        });
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

                try {

                    switch (event.getKind()) {
                        case NEW_FILE:

                             worker.addWork(new Work(event));
                             break;

                        case EDIT_FILE:

                            worker.addWork(new Work(event));
                            break;

                        case REMOVE_FILE:

                            watcher.startIgnoring(file.toFile());

                            file.toFile().delete();

                            watcher.stopIgnoring(file.toFile());

                            break;

                        default:
                            log.warn("New unrecognized sync event from the storage");
                    }
                    rememberEvent(event);
                } catch (Exception ex) {
                    log.warn(ex.toString(), ex);
                }
            }
        });
    }

    private void rememberEvent (SyncEvent eventToRemember) {

        config.setProperty("lastEvent", String.valueOf(eventToRemember.getID()));
        try {
            config.save();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stop the sync object.
     * @throws InterruptedException
     */
    public void shutdown () throws InterruptedException {
        syncState = false;
        watcher.shutdown();
        worker.shutdown();
    }

    public FileSystemWatcher getFileSystemWatcher () {
        return watcher;
    }

    public Worker getWorker () { return worker; }

    public void setSyncing(boolean newState) throws InterruptedException, ClientException, IOException {
        if (newState == syncState)
            throw new IllegalStateException("Already in this state");
        if (newState)
            resyncAndStart();
        else
            shutdown();
    }

    public boolean isSyncing() {
        return syncState;
    }
}