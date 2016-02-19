package it.simonedegiacomi.sync;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Sync object work with an implementation of
 * the Client interface, and manage the synchronization
 * of the filesystem with the relative storage of the account.
 *
 * @author Degiacomi Simone
 * Created on 24/12/2015
 */

public class Sync {

    /**
     * Logger of this class
     */
    private static final Logger log = Logger.getLogger(Sync.class.getName());

    private final Worker worker;
    /**
     * Client object used as API interface tp communicate
     * with the storage.
     */
    private Client client;

    /**
     * Configuration of the environment
     */
    private static final Config config = Config.getInstance();

    /**
     * The FileSystemWatcher is the object that pool and
     * watch the local fileSystem, notifying any creation,
     * changes or deletion of a file (or folder)
     */
    private FileSystemWatcher watcher;

    /**
     * no
     * Path of the files folder
     */
    private static final String PATH = config.getProperty("path");

    /**
     * Create and start keep in sync the local fs with
     * the GoBox Storage. It used the Client passed as
     * arguments to communicate the events and to get
     * the changes from the storage
     * @param client Client used to communicate and get
     *               the files
     */
    public Sync (Client client) {

        this.client = client;
        worker = new Worker(client, Worker.DEFAULT_THREADS);

        // Create the new watcher for the fileSystem
        try {
            Path pathToWatch = new File(PATH).toPath();
            watcher = new FileSystemWatcher(pathToWatch);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }

        // Assign the event of the file watcher
        assignFileWatcherEvents();
    }

    public void resyncAndStart () {
        try {
            checkR(new File(PATH));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Resync completed");

        // Start watching for changes
        watcher.start();

        // And listen for event's from the storage
        assignSyncEventFromStorage();
    }

    private void checkR (File file) throws Exception {
        // Get details about this file
        GBFile gbFile = client.getInfo(new GBFile(file, PATH));
        //GBFile gbFile = client.getInfo(new GBFile(2));


        // If the storage doesn't know anything about this file
        if(gbFile == null) {
            // Upload it
            uploadR(file);
            return;
        }

        // If it's a directory
        if(gbFile.isDirectory()) {

            // Create a map with the name of the file as key and the GBFile
            // as value. These files are the children of the folder
            HashMap<String, GBFile> storageFiles = new HashMap<>();
            for (GBFile child : gbFile.getChildren())
                storageFiles.put(child.getName(), child);


            System.out.println("Map of the storage files:");
            System.out.println(storageFiles);

            // Check every children
            for (File child : file.listFiles()) {
                checkR(child);
                storageFiles.remove(child.getName());
            }

            System.out.println("NOW Map of the storage files:");
            System.out.println(storageFiles);


            // Wait! and the remaining files in the map?
            // This client doesn't have these file!
            for (Map.Entry<String, GBFile> entry : storageFiles.entrySet()) {
                System.out.println("Look, a new file! " + entry.getValue());
                // Download it
                downloadR(entry.getValue());
            }
        } else {
            // If it's not a directory but it's a file
            // check who have the latest version
            if(gbFile.getLastUpdateDate() > file.lastModified())
                downloadR(gbFile);
            else if (gbFile.getLastUpdateDate() < file.lastModified())
                worker.addWork(new Work(gbFile, Work.WorkKind.UPDATE));
        }
    }

    private void downloadR (GBFile fileToDownload) throws Exception {
        System.out.println("Download " + fileToDownload);
        fileToDownload = client.getInfo(fileToDownload);
        if (fileToDownload.isDirectory()) {
            Files.createDirectory(fileToDownload.toFile(PATH).toPath());
            for(GBFile child : fileToDownload.getChildren())
                downloadR(child);
        } else {
            Work work = new Work(fileToDownload, Work.WorkKind.DOWNLOAD);
            worker.addWork(work);
        }
    }

    private void uploadR (File fileToUpload) throws Exception {
        System.out.println("Upload " + fileToUpload);
        if (fileToUpload.isDirectory()) {
            client.createDirectory(new GBFile(fileToUpload, PATH));
            for(File child : fileToUpload.listFiles())
                uploadR(child);
        } else {
            Work work = new Work(new GBFile(fileToUpload, PATH), Work.WorkKind.UPLOAD);
            worker.addWork(work);
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
        // I download the file from the server, and new files found are trasmetted to sevrer.
        // If a file was deleted onEvent the client with the program not running, the file will
        // redownloaded

        // This event is called when a new file or directory is created
        watcher.assignListener(FileSystemWatcher.FILE_CREATED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File newFile) {
                log.fine("New file created in the local fs");
                try {
                    // Wrap the java File into a GoBoxFile
                    GBFile wrappedFile = new GBFile(newFile, PATH);


                    if (wrappedFile.isDirectory())
                        client.createDirectory(wrappedFile);
                    else
                        worker.addWork(new Work(wrappedFile, Work.WorkKind.UPLOAD));

                } catch (ClientException ex) {
                    log.warning("Can't tell the storage about the new file");
                }
            }
        });

        // Event thrown when the file is edited
        watcher.assignListener(FileSystemWatcher.FILE_CHANGED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File editedFile) {
                log.fine("New file updated onEvent the local fs");

                try {
                    // Wrap the java File into a GoBoxFile
                    GBFile wrappedFile = new GBFile(editedFile, PATH);

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
                    GBFile wrappedFile = new GBFile(deletedFile, PATH);

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
     * that will listen at the events transmitted from
     * the storage. These events are the result of the
     * operation onEvent the files from other clients.
     */
    private void assignSyncEventFromStorage () {

        // Set the listener
        client.setSyncEventListener(new SyncEventListener() {

            @Override
            public void on(SyncEvent event) {

                // Get the GBFile of this event
                GBFile file = event.getRelativeFile();

                System.out.println("New event: " + file.toString());

                // Tell the file system watcher to ignore the event to this specific file
                // otherwise a infinite loop will be generated. Very, very bad thing
                watcher.ignore(file.toFile(PATH));

                try {
                    switch (event.getKind()) {
                        case NEW_FILE:

                            // If the event is the creation of a new file
                            if (file.isDirectory())
                                // and is a new folder, just create it
                                Files.createDirectories(file.toFile(PATH).toPath());
                            else
                                // otherwise download the new file
                                worker.addWork(new Work(event));
                            break;

                        case EDIT_FILE:

                            worker.addWork(new Work(event));
                            break;

                        case REMOVE_FILE:
                            file.toFile(PATH).delete();
                            break;
                        default:
                            log.warning("New unrecognized sync event from the storage");
                    }
                    rememberEvent(event);
                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
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
}