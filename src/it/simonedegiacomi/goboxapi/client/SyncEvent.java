package it.simonedegiacomi.goboxapi.client;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import it.simonedegiacomi.goboxapi.GBFile;

/**
 * This class is used to create the SyncEvent object
 * that contain the information about a new event made
 * from another client onEvent the storage
 *
 * Created by Degiacomi Simone onEvent 02/01/16.
 */
@DatabaseTable(tableName = "event")
public class SyncEvent implements Comparable {

    /**
     * Kinds of events
     */
    public enum EventKind { NEW_FILE, EDIT_FILE, REMOVE_FILE};

    /**
     * ID of the event
     */
    @DatabaseField(generatedId = true, canBeNull = false)
    private long ID;

    /**
     * Kind of this event
     */
    @DatabaseField(canBeNull = false)
    private EventKind kind;

    /**
     * File associated with this event
     */
    @DatabaseField(foreign = true)
    private GBFile file;

    @DatabaseField
    private long date;

    public SyncEvent () { }

    public SyncEvent(EventKind kind, GBFile relativeFile) {
        this.kind = kind;
        setRelativeFile(relativeFile);
    }

    public SyncEvent(EventKind kind) {
        this.kind = kind;
    }

    /**
     * Return the kind of the event as a string
     * @return string that represent the kind of the event
     */
    public String getKindAsString() { return kind.toString();}

    public EventKind getKind () { return kind; }

    public GBFile getRelativeFile(){
        return file;
    }

    /**
     * Set the file associated with this event
     * @param relativeFile File associated with this event
     */
    public void setRelativeFile(GBFile relativeFile) {
        this.file = relativeFile;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public int compareTo(Object o) {
        return o == this ? 0 : 1;
    }
}
