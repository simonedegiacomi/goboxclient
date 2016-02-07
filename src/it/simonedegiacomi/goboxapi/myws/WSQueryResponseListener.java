package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;

/**
 * Listener for the response ogf a query
 *
 * Created by Degiacomi Simone onEvent 31/12/2015.
 */
public interface WSQueryResponseListener {

    /**
     * Method that will be called when the response of a
     * query is retrieved
     * @param response Response of the query
     */
    public void onResponse (JsonElement response);
}
