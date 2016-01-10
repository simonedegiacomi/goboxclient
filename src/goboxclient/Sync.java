package goboxclient;

import configuration.Config;
import goboxapi.GBFile;
import goboxapi.client.Client;
import goboxapi.client.ClientException;
import goboxapi.client.SyncEvent;
import goboxapi.client.SyncEventListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Sync object os build on implementation of
 * goboxapi.Client, and manage the synchronization
 * of the filesystem with the relative storage of
 * the account.
 */

public class Sync {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(Sync.class.getName());

    /**
     * Client object used as API interface tp comunicate
     * with the storage.
     */
    private Client client;

    /**
     * Configuration of the enviroment
     */
    private final Config config = Config.getInstance();

    /**
     * The FileSystemWatchet is the object that pool and
     * watch the local fileSystem, notifying any creation,
     * changes or deletion of a file (or folder)
     */
    private FileSystemWatcher watcher;

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

        // And start a separate thread that will pool
        // continuously the filesystem
        watcher.start();

        // Assing event listener for the SyncEvent from the storage
        assignSyncEventFromStorage();
    }

    /**
     * This method assign action for the possible events of
     * the fileSystemWatcher. For each event the relative
     * method of the client ( the object that wraps the API of
     * the storage) will be called.
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
                    log.warning("Cannot tell the storage about the new file");
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

    private void assignSyncEventFromStorage () {
        client.setSyncEventListener(new SyncEventListener() {

            @Override
            public void on(SyncEvent event) {
                GBFile file = event.getRelativeFile();
                try {
                    switch (event.getKind()) {
                        case SyncEvent.CREATE_FILE:
                            if (file.isDirectory())
                                createDirectory(file);
                            else
                                client.getFile(file);
                            break;
                        default:
                            log.warning("New unrecognized sync event from the storage");
                    }
                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        });
    }
}