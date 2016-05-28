package it.simonedegiacomi.storage.components.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;

/**
 * Created on 5/28/16.
 * @author Degiacomi Simone
 */
public class Ping implements GBComponent {

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {

    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "ping")
    public JsonElement onPingQuery (JsonElement data) {
        return new JsonObject();
    }
}
