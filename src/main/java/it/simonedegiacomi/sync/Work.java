package it.simonedegiacomi.sync;

import com.google.common.io.Files;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.security.InvalidParameterException;

/**
 * This class manage the upload/download with the concept of a work queue
 * Created on 17/02/16.
 * @author Degiacomi Simone
 */
public class Work {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(Work.class);

    /**
     * Kinds of works
     */
    public enum WorkKind {

        /**
         * Download a file fromthe storage
         */
        DOWNLOAD,

        /**
         * Upload a file to the storage
         */
        UPLOAD,

        /**
         * Tell the storage to move (or copy) a file
         */
        MOVE_IN_STORAGE,

        /**
         * Move a file in the local file system
         */
        MOVE_IN_CLIENT,

        /**
         * Tell the storage to remove a file
         */
        REMOVE_IN_STORAGE,

        /**
         * Remove a file in the client
         */
        REMOVE_IN_CLIENT
    }

    /**
     * State of works
     */
    public enum WorkState {

        /**
         * The work is waiting in the queue
         */
        QUEUE,

        /**
         * The work is running
         */
        RUNNING,

        /**
         * The work i successfully completed
         */
        END,

        /**
         * Work failed and no longer running
         */
        FAILED
    }

    /**
     * Kind of this work
     */
    private final WorkKind kind;

    /**
     * Current state of the work
     */
    private WorkState state = WorkState.QUEUE;

    /**
     * Used with the MOVE work
     */
    private GBFile before;

    /**
     * File of the work
     */
    private final GBFile file;

    /**
     * Client to use to complete the work
     */
    private Client client;

    /**
     * File system watcher that listen for new changes on the local fs
     */
    private static FileSystemWatcher watcher;

    /**
     * Worker used in the environment. This is used to schedule new work
     */
    private static Worker worker;

    /**
     * Set the file system watcher
     * @param watcher File system watcher
     */
    public static void setWatcher (FileSystemWatcher watcher) {
        Work.watcher = watcher;
    }

    /**
     * Set the Worker used in the environment
     * @param worker Current used worker
     */
    public static void setWorker (Worker worker) {
        Work.worker = worker;
    }

    /**
     * Create a new work from a SyncEvent received from the storage.
     * @param event SyncEvent (receiver from the storage) from which create a new work
     */
    public Work (SyncEvent event) {

        // Keep a reference to the relative file
        this.file = event.getRelativeFile();

        // Select the right work kind
        switch (event.getKind()) {

            case FILE_CREATED:

                // Download the new file
                kind = WorkKind.DOWNLOAD;
                break;

            case FILE_MODIFIED:

                // Rename the file
                kind = WorkKind.DOWNLOAD;
                before = event.getBefore();
                break;

            case FILE_MOVED:

                // Download the new version
                kind = WorkKind.MOVE_IN_CLIENT;
                before = event.getBefore();
                break;

            case FILE_DELETED:

                // Delete
                kind = WorkKind.REMOVE_IN_CLIENT;
                break;

            default:

                kind = WorkKind.DOWNLOAD;
                break;
        }
    }

    /**
     * Create a new work specifying the kind
     * @param file File related to the work
     * @param kind Kind of work
     */
    public Work (GBFile file, WorkKind kind) {
        this.file = file;
        this.kind = kind;
    }

    /**
     * Set the 'before' file. This is used in the move work, to know the original location of the file
     * @param before
     */
    public void setBefore (GBFile before) {
        this.before = before;
    }

    /**
     * Return the runnable object that do the right thing to complete the work
     * @param client Client to use to complete the work
     * @return Runnable to execute to complete
     */
    public Runnable getWork (Client client) {

        this.client = client;

        return () -> {

            // Change state of the work
            state = WorkState.RUNNING;

            // Tell the watcher to ignore the event
            watcher.startIgnoring(file.toFile());

            try {
                switch (kind) {
                    case DOWNLOAD:
                        download(file);
                        break;

                    case UPLOAD:
                        upload(file);
                        break;

                    case MOVE_IN_CLIENT:

                        // TODO: implement
                        break;

                    case MOVE_IN_STORAGE:

                        // TODO: implement
                        break;

                    case REMOVE_IN_CLIENT:

                        MyFileUtils.delete(file);
                        break;

                    case REMOVE_IN_STORAGE:

                        client.removeFile(file);
                        break;
                }
            } catch (Exception ex) {

                // Change the state
                state = WorkState.FAILED;

                // Log the exception
                log.warn("work failed", ex);
            }

            // Stop ignoring the file
            watcher.stopIgnoring(file.toFile());

            // Change state of work
            state = WorkState.END;
        };
    }

    /**
     * Return the current state of the work
     * @return Current state of the work
     */
    public WorkState getState () {
        return state;
    }

    /**
     * Download the file if a real file, schedule multiple download if a folder
     * @param file File to download
     * @throws IOException
     * @throws ClientException
     */
    private void download (GBFile file) throws IOException, ClientException {

        // If the file is a directory
        if (file.isDirectory()) {

            // Download each file
            for (GBFile child : file.getChildren()) {
                worker.addWork(new Work(child, WorkKind.DOWNLOAD));
            }
        }

        // Otherwise just download the file
        client.getFile(file);
    }

    /**
     * Download the file if a file, schedule multiple upload if a folder
     * @param file File to upload
     * @throws ClientException
     * @throws IOException
     */
    private void upload (GBFile file) throws ClientException, IOException {

        // If the file is a folder
        if (file.isDirectory()) {

            // Create the folder in the storage
            client.createDirectory(file);

            // And create enw works, to upload each file
            for (GBFile child : file.getChildren()) {
                worker.addWork(new Work(child, WorkKind.UPLOAD));
            }

            return;
        }

        // Upload the file
        client.uploadFile(file);
    }
}