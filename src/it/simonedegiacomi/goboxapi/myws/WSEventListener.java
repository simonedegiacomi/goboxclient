package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;

/**
 * Interface to implement to create a new event listener
 * for the handlers socket client.
 *
 * Created by Degiacomi Simone onEvent 28/12/15.
 */
public interface WSEventListener {

    /**
     * Method that will be called when the a message with
     * this event is received
     * @param data Data of the event
     */
    public void onEvent (JsonElement data);
}
