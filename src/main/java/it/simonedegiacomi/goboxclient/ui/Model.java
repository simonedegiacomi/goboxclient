package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.sync.Work;

import java.util.List;

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
    ClientState getClientState ();

    /**
     * This method connects the client
     */
    void connectClient ();

    /**
     * This method stops the client
     */
    void shutdownClient ();

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
    List<Work> getWorks();

    /**
     * Return true if the progrm is running in storage mode
     */
    boolean isStorageMode ();

    /**
     * Stop the works, shutdown the client and close the program
     */
    void exitProgram ();
}