package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;

/**
 * This class manage the upload/download with the concept of a work queue
 * Created on 17/02/16.
 * @author Degiacomi Simone
 */
public class Work {

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

            case FILE_MOVED:

                // Rename the file
                kind = WorkKind.DOWNLOAD;
                before = event.getBefore();
                break;

            case FILE_MODIFIED:

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
     * Return the current state of the work
     * @return Current state of the work
     */
    public WorkState getState () {
        return state;
    }

    public void setState (WorkState state) { this.state = state; }

    public WorkKind getKind() {
        return kind;
    }

    public GBFile getBefore() {
        return before;
    }

    public GBFile getFile() {
        return file;
    }

    public String toString () {
        return kind + " file: " + file;
    }
}