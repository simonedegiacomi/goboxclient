package it.simonedegiacomi.goboxapi.myws;

import org.json.JSONObject;

/**
 * Listener for the response ogf a query
 *
 * Created by Degiacomi Simone on 31/12/2015.
 */
public interface WSQueryResponseListener {

    /**
     * Method that will be called when the response of a
     * query is retived
     * @param response Response of the query
     */
    public void onResponse (JSONObject response);
}
