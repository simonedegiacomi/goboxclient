package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Sync object work with an implementation of
 * the Client interface, and manage the synchronization
 * of the filesystem with the relative it.simonedegiacomi.storage of
 * the account.
 *
 * Created by Degiacomi Simone on 24/12/2015
 */

public class Sync {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(Sync.class.getName());

    /**
     * Client object used as API interface tp communicate
     * with the it.simonedegiacomi.storage.
     */
    private Client client;

    /**
     * Configuration of the environment
     */
    private final Config config = Config.getInstance();

    /**
     * The FileSystemWatcher is the object that pool and
     * watch the local fileSystem, notifying any creation,
     * changes or deletion of a file (or folder)
     */
    private FileSystemWatcher watcher;

    /**
     * This array contains the id of the file to ignore. Is used
     * when to ignore these notification from the it.simonedegiacomi.storage that
     * speak about the new file I (this instance) has created
     */
    private ArrayList<Long> filesToIgnore = new ArrayList<>();

    /**
     * Create and start keep in sync the local fs with
     * the GoBox Storage. It used the Client passed as
     * arguments to communicate the events and to get
     * the changes from the it.simonedegiacomi.storage
     * @param client Client used to communicate and get
     *               the files
     */
    public Sync (Client client) {

        this.client = client;

        // Create the new watcher for the fileSystem
        try {
            Path pathToWatch = new File("files/").toPath();
            watcher = new FileSystemWatcher(pathToWatch);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }

        // Assign the event of the file watcher
        assignFileWatcherEvents();
    }

    /**
     * This method merge the local fs with the files of the
     * it.simonedegiacomi.storage. This is needed when the client was offline or
     * some files are changed while the client wasn't running.
     */
    public void mergeWithStorage () throws ClientException {
        // Download every folder and checl for the lastes version
        mergeR(new GBFile("root", GBFile.ROOT_ID, true));
    }

    private void mergeR (GBFile father) throws ClientException{
        GBFile[] files = client.listDirectory(father);
        for(GBFile file : files) {

            if(!file.toFile().exists()) {
                // If the file exist in the local fs
            } else {
                // If it doesn't exist
            }

            // If is a folder, go deeper
            if(file.toFile().isDirectory())
                mergeR(file);
        }
    }

    /**
     * Start the watching thread and the listener from the it.simonedegiacomi.storage
     */
    public void startSync () {

        // And start a separate thread that will pool
        // continuously the filesystem
        watcher.start();

        // Assign event listener for the SyncEvent from the it.simonedegiacomi.storage
        assignSyncEventFromStorage();
    }

    /**
     * Stop the synchronization with the it.simonedegiacomi.storage
     */
    public void stopSync () {

        // Stop the thread that watch the local fs
        // TODO: Stop the thread

        // and remove the event listener
        client.setSyncEventListener(null);
    }

    /**
     * This method assign action for the possible events of
     * the fileSystemWatcher. For each event the relative
     * method of the client ( the object that wraps the API of
     * the it.simonedegiacomi.storage) will be called.
     */
    private void assignFileWatcherEvents () {
        // At the beginning i download the file list, and make a control.
        // I download the file from the server, and new files found are trasmetted to sevrer.
        // If a file was deleted on the client with the program not running, the file will
        // redownloaded

        // This event is called when a new file or directory is created
        watcher.assignListener(FileSystemWatcher.FILE_CREATED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File newFile) {
                log.fine("New file created in the local fs");
                try {
                    // Wrap the java File into a GoBoxFile
                    GBFile wrappedFile = new GBFile(newFile);

                    if (wrappedFile.isDirectory()) {

                        // Create the folder
                        client.createDirectory(wrappedFile);
                    } else {

                        // Upload the file to the server
                        client.uploadFile(wrappedFile);
                    }

                } catch (ClientException ex) {
                    log.warning("Cannot tell the it.simonedegiacomi.storage about the new file");
                }
            }
        });

        // Event thrown when the file is edited
        watcher.assignListener(FileSystemWatcher.FILE_CHANGED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File editedFile) {
                log.fine("New file updated on the local fs");

                try {
                    // Wrap the java File into a GoBoxFile
                    GBFile wrappedFile = new GBFile(editedFile);

                    // Call the right client method
                    client.updateFile(wrappedFile);
                } catch (ClientException ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });

        // Event called when a file is removed
        watcher.assignListener(FileSystemWatcher.FILE_DELETED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File deletedFile) {
                log.fine("A file in the local fs was deleted");

                try {
                    // Wrap the file
                    GBFile wrappedFile = new GBFile(deletedFile);
                    // and remove it
                    client.removeFile(wrappedFile);
                } catch (ClientException ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });
    }

    /**
     * This method set the listener of the client object
     * that will listen at the events trasmitted from
     * the it.simonedegiacomi.storage. These events are the result of the
     * operation on the files from other clients.
     */
    private void assignSyncEventFromStorage () {

        // Set the listener
        client.setSyncEventListener(new SyncEventListener() {

            @Override
            public void on(SyncEvent event) {

                // Get the GBFile of this event
                GBFile file = event.getRelativeFile();

                // Check if i should ignore this event
                if (filesToIgnore.remove(new Long(file.getID())))
                    return;

                try {
                    switch (event.getKind()) {
                        case NEW_FILE:

                            // If the event is the creation of a new file
                            if (file.isDirectory())
                                // and is a new folder, just create it
                                createDirectory(file);
                            else
                                // otherwise download the new file
                                client.getFile(file);
                            break;

                        case EDIT_FILE:
                            break;

                        case REMOVE_FILE:

                            break;
                        default:
                            log.warning("New unrecognized sync event from the it.simonedegiacomi.storage");
                    }
                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });
    }

    /**
     * This method create a new directory on the local fs.
     * Normally this is called when a new folder is created
     * by another client
     * @param newDir New directory to create
     * @throws IOException
     */
    private void createDirectory (GBFile newDir) throws IOException {
        Files.createDirectory(newDir.toPath());
    }

    /**
     * Remove a file on the local fs. This method, like the
     * 'createDirectory' is called when the delete event is
     * received from  the it.simonedegiacomi.storage
     * @param fileToRemove File to delete
     * @throws IOException
     */
    private void removeFile (GBFile fileToRemove) throws IOException {

    }
}