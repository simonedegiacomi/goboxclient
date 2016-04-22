package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import org.apache.log4j.Logger;

/**
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
    public enum WorkKind { DOWNLOAD, UPLOAD, UPDATE };

    /**
     * State of works
     */
    public enum WorkState { QUEUE, RUNNING, END, FAILED }

    /**
     * Kind of this work
     */
    private final WorkKind kind;

    /**
     * Current state of the work
     */
    private WorkState state = WorkState.QUEUE;

    /**
     * File of the work
     */
    private final GBFile file;

    /**
     * File system watcher that listen for new changes on the local fs
     */
    private static FileSystemWatcher watcher;

    /**
     * Set the file system watcher
     * @param watcher File system watcher
     */
    public static void setWatcher (FileSystemWatcher watcher) {

        Work.watcher = watcher;
    }

    /**
     * Create a new work from a SyncEvent
     * @param event SyncEvent from which create a new work
     */
    public Work (SyncEvent event) {

        // Keep a reference to the relative file
        file = event.getRelativeFile();

        switch (event.getKind()) {

            case NEW_FILE:

                kind = WorkKind.DOWNLOAD;
                break;
            case EDIT_FILE:

                kind = WorkKind.DOWNLOAD;
                break;
            default:

                kind = WorkKind.DOWNLOAD;
        }
    }

    public Work (GBFile file, WorkKind kind) {

        this.file = file;
        this.kind = kind;
    }

    /**
     * Return the runnable object that do the right thing to complete the work
     * @param client Client to use to complete the work
     * @return Runnable to execute to complete
     */
    public Runnable getWork (Client client) {

        return new Runnable() {

            @Override
            public void run() {

                // Change state of the work
                state = WorkState.RUNNING;

                // Tell the watcher to ignore the event
                watcher.startIgnoring(file.toFile());

                try {
                    switch (kind) {
                        case DOWNLOAD:

                            client.getFile(file);
                            break;
                        case UPLOAD:

                            client.uploadFile(file);
                            break;
                        case UPDATE:

                            client.updateFile(file);
                            break;
                    }
                } catch (Exception ex) {

                    // Change the state
                    state = WorkState.FAILED;

                    // Log the exception
                    log.warn("Work execution failed (" + ex.toString() + ")");
                }

                // Stop ignoring the file
                watcher.stopIgnoring(file.toFile());

                // Change state of work
                state = WorkState.END;
            }
        };
    }

    /**
     * Return the current state of the work
     * @return Current state of the work
     */
    public WorkState getState () {
        return state;
    }
}