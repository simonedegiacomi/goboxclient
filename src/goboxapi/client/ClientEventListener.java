package goboxapi.client;

import goboxapi.GBFile;
import goboxclient.SyncEvent;

/**
 * Created by Simone on 01/01/2016.
 */
public interface ClientEventListener {
    public void onEvent (SyncEvent event);
}
