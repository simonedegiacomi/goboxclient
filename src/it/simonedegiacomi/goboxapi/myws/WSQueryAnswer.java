package it.simonedegiacomi.goboxapi.myws;


import com.google.gson.JsonElement;

/**
 * Interface to implement to create a new QueryListener
 * for the web socket client.
 * This is commonly used in a server (but that is a client...
 * like the storage, a client that serve contents!)
 *
 * Created by Degiacomi Simone on 30/12/2015.
 */
public interface WSQueryAnswer {

    /**
     * Method that will call when a query with this name is received
     * @param data Data of the query
     * @return Response of the query
     */
    public JsonElement onQuery (JsonElement data);
}
