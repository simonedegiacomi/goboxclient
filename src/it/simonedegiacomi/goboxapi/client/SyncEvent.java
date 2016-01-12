package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.GBFile;
import org.json.JSONObject;

/**
 * This class is used to create the SyncEvent object
 * that contain the information about a new event made
 * from another client on the it.simonedegiacomi.storage
 *
 * Created by Degiacomi Simone on 02/01/16.
 */
public class SyncEvent {

    /**
     * Kinds of events
     */
    public enum EventKind { NEW_FILE, EDIT_FILE, REMOVE_FILE};

    /**
     * Kind of this event
     */
    private EventKind kind;

    /**
     * File associated with this event
     */
    private GBFile relativeFile;

    public SyncEvent(EventKind kind, GBFile relativeFile) {
        this.kind = kind;
        this.relativeFile = relativeFile;
    }

    public SyncEvent (JSONObject obj) {
        try {
            this.kind = EventKind.valueOf(obj.getString("kind"));
            this.relativeFile = new GBFile(obj.getJSONObject("file"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SyncEvent(EventKind kind) {
        this.kind = kind;
    }

    /**
     * Return the kind of the event as a string
     * @return string representating the kind of the event
     */
    public String getKindAsString() { return kind.toString();}

    public EventKind getKind () { return kind; }

    public GBFile getRelativeFile() {
        return relativeFile;
    }

    /**
     * Create the JSON (JSONObject) representation of this event
     * @return JSON representation of this event
     */
    public JSONObject toJSON () {
        JSONObject obj = new JSONObject();
        try {
            obj.put("kind", kind);
            obj.put("file", relativeFile.toJSON());
        } catch (Exception ex) {
            ex.toString();
        }
        return obj;
    }

    /**
     * Set the file associated with this event
     * @param relativeFile File associated with this event
     */
    public void setRelativeFile(GBFile relativeFile) {
        this.relativeFile = relativeFile;
    }
}
