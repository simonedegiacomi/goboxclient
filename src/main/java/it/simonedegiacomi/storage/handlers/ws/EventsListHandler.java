package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;

import java.util.List;

/**
 * Created by simone on 08/02/16.
 */
public class EventsListHandler implements WSQueryHandler {

    private final StorageDB db;

    private final Gson gson = new Gson();

    public EventsListHandler (StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "getEventsList")
    @Override
    public JsonElement onQuery(JsonElement data) {
        long lastHeardId = ((JsonObject) data).get("id").getAsLong();
        List<SyncEvent> events = db.getUniqueEventsFromID(lastHeardId);
        // Return the gson tree. To create this tree, i need to implement a new TypToken
        // and instantiate it. As you can see the implementation is empty, but doing that
        // i can get the type. (this because i can't use List<SyncEvent>.class . If you want to know
        // more google 'Type Erasure Java')
        return gson.toJsonTree(events, new TypeToken<List<SyncEvent>>() {}.getType());
    }
}
