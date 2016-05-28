package it.simonedegiacomi.storage;

import com.google.gson.Gson;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 07/02/16.
 * @author Degiacomi Simone
 */
public class EventEmitter {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(EventEmitter.class);

    /**
     * WS CLient, used to send the messages
     */
    private final MyWSClient clients;

    private final Set<SyncEventListener> internalListeners = new HashSet<>();

    /**
     * Gson used to serialize events
     */
    private final Gson gson = new Gson();

    public EventEmitter (MyWSClient ws) {
        this.clients = ws;
    }

    /**
     * Send the event to all the other clients
     * @param eventToEmit
     */
    public void emitEvent (SyncEvent eventToEmit) {
        log.info("emit new event " + eventToEmit);

        // Send the event to all the clients
        clients.sendEventBroadcast("syncEvent", gson.toJsonTree(eventToEmit, SyncEvent.class));

        for (SyncEventListener listener : internalListeners) {
            listener.on(eventToEmit);
        }
    }

    public void addInternalListener(SyncEventListener internalListener) {
        internalListeners.add(internalListener);
    }
}