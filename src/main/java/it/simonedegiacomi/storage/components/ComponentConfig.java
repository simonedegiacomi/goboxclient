package it.simonedegiacomi.storage.components;

import it.simonedegiacomi.configuration.Config;

/**
 * Created on 26/05/16.
 * @author Degiacomi Simone
 */
public class ComponentConfig {

    private final Config father;

    private final String prefix = "default";

    public ComponentConfig (Config father) {
        this.father = father;
    }

    public void addPropetry (String key, String value) {
        father.setProperty(prefix + key, value);
    }

    public String getProperty (String key, String defaultValue) {
        return father.getProperty(key, defaultValue);
    }
}