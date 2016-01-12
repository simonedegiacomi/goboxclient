package it.simonedegiacomi.goboxapi.myws;

import org.json.JSONObject;

/**
 * Interface to implement to create a new event listener
 * for thw eb socket client
 *
 * Created by Degiacomi Simone on 28/12/15.
 */
public interface WSEventListener {

    /**
     * Method that will be called when the a message with
     * this event is received
     * @param data Data of the event
     */
    public void onEvent (JSONObject data);
}
