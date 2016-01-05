package goboxapi.client;

import goboxapi.GBFile;
import goboxapi.authentication.Auth;
import java.io.InputStream;

/**
 * Created by Simone on 02/01/2016.
 */
public interface Client {

    public InputStream getFile (GBFile file);

    public void uploadFile (GBFile file);

    public void removeFile (GBFile file);

    public void updateFile (GBFile file);

    public void assignEventListener (ClientEventListener listener);
}
