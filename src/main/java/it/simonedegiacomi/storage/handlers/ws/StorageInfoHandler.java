package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by simone on 19/03/16.
 */
public class StorageInfoHandler implements WSQueryHandler {

    private final Gson gson = new MyGsonBuilder().create();

    private final Map<String, String> infos = new HashMap<>();

    @WSQuery(name = "storageInfo")
    @Override
    public JsonElement onQuery(JsonElement data) {
        return gson.toJsonTree(infos, new TypeToken<Map<String, String>>(){}.getType());
    }

    public void addInfo (String infoName, String infoValue) {
        infos.put(infoName, infoValue);
    }
}
