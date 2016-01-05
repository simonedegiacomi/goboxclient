package goboxapi.MyWS;

import org.json.JSONObject;

/**
 * Created by Degiacomi Simone on 28/12/15.
 */
public interface WSEvent {
    public void onEvent (JSONObject data);
}
