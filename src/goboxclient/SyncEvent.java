package goboxclient;

import goboxapi.GBFile;

/**
 * Created by simone on 02/01/16.
 */
public class SyncEvent {

    public static final int CREATE_FILE = 0;
    public static final int EDIT_FILE = 1;
    public static final int REMOVE_FILE = 2;

    private final int kind;
    private GBFile relativeFile;

    public SyncEvent(int kind, GBFile relativeFile) {
        this.kind = kind;
        this.relativeFile = relativeFile;
    }

    public SyncEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public GBFile getRelativeFile() {
        return relativeFile;
    }

    public void setRelativeFile(GBFile relativeFile) {
        this.relativeFile = relativeFile;
    }
}
