package it.simonedegiacomi.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

/**
 * Created by simone on 21/02/16.
 */
public class MyGson {

    private GsonBuilder builder = new GsonBuilder();

    public MyGson () {
        builder.setLongSerializationPolicy(LongSerializationPolicy.STRING);
    }

    public Gson create () {
        return builder.create();
    }
}
