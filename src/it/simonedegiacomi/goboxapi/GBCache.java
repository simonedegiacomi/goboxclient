package it.simonedegiacomi.goboxapi;

import java.util.HashMap;

/**
 * Created by simone on 18/02/16.
 */
public class GBCache {
    private final HashMap<Long, GBFile> cacheById = new HashMap<>();
    private final HashMap<String, GBFile> cacheByPath = new HashMap<>();

    public void add (GBFile file) {
        if(file.getID() != GBFile.UNKNOWN_ID)
            cacheById.put(file.getID(), file);
        cacheByPath.put(file.getPathAsString(), file);
        for(GBFile child : file.getChildren())
            if(!child.isDirectory())
                add(child);
    }

    public GBFile get (long id) {
        return cacheById.get(id);
    }

    public GBFile get (String path) {
        return cacheByPath.get(path);
    }

    public GBFile get (GBFile poorFile) {
        if(poorFile.getID() != GBFile.UNKNOWN_ID && cacheById.containsKey(poorFile.getID()))
            return cacheById.get(poorFile.getID());
        return cacheByPath.get(poorFile.getPathAsString());
    }
}
