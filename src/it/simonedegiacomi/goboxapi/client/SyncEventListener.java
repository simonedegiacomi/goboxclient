package it.simonedegiacomi.goboxapi.client;

/**
 * Interface used to create the listeners of the sync events
 *
 * Created by Degiacomi Simone on 01/01/2016.
 */
public interface SyncEventListener {

    /**
     * Method that will be called when a new event is received
     * @param event Event
     */
    public void on (SyncEvent event);
}
