package it.simonedegiacomi.goboxapi.myws;

import org.json.JSONObject;

import java.util.concurrent.Callable;

/**
 * Implementation of Callable used in my implementation
 * of web socket client for the java FutureTask
 *
 * Created by Degiacomi Simone on 10/01/16.
 */
public class WSCallable implements Callable<JSONObject> {

    /**
     * This field contains the response retrived from the server
     */
    private JSONObject response;

    /**
     * Set the response. This method must be called befor execute the
     * call method
     * @param response
     */
    public void setResponse (JSONObject response) {
        this.response = response;
    }

    @Override
    public JSONObject call() throws Exception {

        // This is a very hard task...
        return response;
    }
}
