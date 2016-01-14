package it.simonedegiacomi.configuration;


import java.awt.GraphicsEnvironment;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public abstract class ConfigTool {

    //public ConfigTool(Config config, EventListener callback);

    public static ConfigTool getConfigTool (EventListener callback) {
        if(GraphicsEnvironment.isHeadless())
            return new ConsoleConfigTool(callback);
        else
            return new GUIConfigTool(callback);
    }

    public interface EventListener  {
        public void onConfigComplete();
        public void onConfigFailed();
    }
}
