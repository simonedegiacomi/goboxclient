package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.sync.Work;

import java.util.Set;

/**
 * This is the Interface Model in the MVP.
 * The model is an interface that defines the data to display and the action that the user
 * can perform. Actually This class take care of the data to display in the view (by the Presenter)
 * and exposes the methods that can be called by the view (always trough the Presenter).
 *
 * Created on 4/20/16.
 * @author Degiacomi Simone
 */
public interface Model {

    /**
     * Return the current state of the client
     * @return
     */
    GBClient.ClientState getClientState ();

    /**
     * This method connects the client
     */
    void connect ();

    /**
     * This method stops the sync, client and if in the appropriate state, the storage
     */
    void shutdown ();

    /**
     * Return true if the storage is connected
     * @return Connection of the storage
     */
    boolean isStorageConnected ();

    /**
     * Check if the sync object is syncing
     * @return
     */
    boolean isSyncing ();

    /**
     * Start or stop the sync action
     * @param sync Sync on or off
     */
    void setSyncing (boolean sync);

    /**
     * Return the list of the running works
     * @return ist of current works
     */
    Set<Work> getCurrentWorks();

    /**
     * Return true if the program is running in storage mode
     */
    boolean isStorageMode ();


    /**
     * Set the message of the current action
     * @param s Message of current action
     */
    void setFlashMessage(String s);

    String getFlashMessage();

    void setError (String error);

    String getError();

    void addOnUpdateListener(Runnable runnable);
}