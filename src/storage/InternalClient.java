package storage;

import goboxapi.GBFile;
import goboxapi.client.Client;
import goboxapi.client.SyncEventListener;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Simone on 02/01/2016.
 */
public class InternalClient implements Client {

    private final StorageDB db;

    public InternalClient(StorageDB db) {
        this.db = db;
    }

    @Override
    public InputStream getFile(GBFile file) {
        return null;
    }

    @Override
    public void getFile(GBFile file, OutputStream dst) {

    }

    @Override
    public void uploadFile(GBFile file, InputStream stream) {

    }

    @Override
    public void uploadFile(GBFile file) {
        try {
            db.insertFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeFile(GBFile file) {
        try {
            db.removeFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {

    }

    @Override
    public void updateFile(GBFile file) {
        try {
            db.updateFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setSyncEventListener (SyncEventListener listener) {

    }

    @Override
    public void assignEventListener (SyncEventListener listener) {

    }
}
