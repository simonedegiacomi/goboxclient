package it.simonedegiacomi.storage;

import com.google.gson.Gson;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;

/**
 * Created by Degiacomi Simone on 07/02/16.
 */
public class EventEmitter {

    private final MyWSClient clients;

    private final Gson gson = new Gson();

    public EventEmitter (MyWSClient ws) {
        this.clients = ws;
    }

    /**
     * Send the event to all the other clients
     * @param eventToEmit
     */
    public void emitEvent (SyncEvent eventToEmit) {

        // Send the event to all the clients
        clients.sendEventBroadcast("syncEvent", gson.toJsonTree(eventToEmit, SyncEvent.class));
    }
}