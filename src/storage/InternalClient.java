package storage;

import goboxapi.GBFile;
import goboxapi.client.Client;
import goboxapi.client.ClientEventListener;

import java.io.InputStream;

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
    public void updateFile(GBFile file) {
        try {
            db.updateFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void assignEventListener(ClientEventListener listener) {

    }
}
