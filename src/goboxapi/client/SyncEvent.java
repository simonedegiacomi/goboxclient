package goboxapi.client;

import goboxapi.GBFile;
import org.json.JSONObject;

/**
 * Created by Degiacomi Simone on 02/01/16.
 */
public class SyncEvent {

    public static final int CREATE_FILE = 0;
    public static final int EDIT_FILE = 1;
    public static final int REMOVE_FILE = 2;

    private int kind;
    private GBFile relativeFile;

    public SyncEvent(int kind, GBFile relativeFile) {
        this.kind = kind;
        this.relativeFile = relativeFile;
    }

    public SyncEvent (JSONObject obj) {
        try {
            this.kind = obj.getInt("kind");
            this.relativeFile = new GBFile(obj.getJSONObject("file"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SyncEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public GBFile getRelativeFile() {
        return relativeFile;
    }

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

    public void setRelativeFile(GBFile relativeFile) {
        this.relativeFile = relativeFile;
    }
}
