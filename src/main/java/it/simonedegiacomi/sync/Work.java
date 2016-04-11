package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.SyncEvent;

import java.io.IOException;

/**
 * Created on 17/02/16.
 * @author Degiacomi Simone
 */
public class Work {

    /**
     * Kinds of work
     */
    public enum WorkKind { DOWNLOAD, UPLOAD, UPDATE };

    /**
     * Kind of this work
     */
    private final WorkKind kind;

    /**
     * File of the work
     */
    private final GBFile file;

    private static FileSystemWatcher watcher;

    public static void setWatcher (FileSystemWatcher watcher) {

        Work.watcher = watcher;
    }

    /**
     * Create a new work from a SyncEvent
     * @param event SyncEvent from which create a new work
     */
    public Work (SyncEvent event) {
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

    public Runnable getWork (Client client) {
        return new Runnable() {
            @Override
            public void run() {

                // Tell the watcher to ignore the event
                watcher.startIgnoring(file);

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
                } catch (ClientException ex) {
                    // TODO: handle this
                    ex.printStackTrace();
                } catch (IOException ex) {
                    // TODO: handle this
                    ex.printStackTrace();
                }

                watcher.stopIgnoring(file);
            }
        };
    }

}
