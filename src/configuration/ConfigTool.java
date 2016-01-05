package configuration;

import java.awt.*;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public abstract class ConfigTool {

    //public ConfigTool(Config config, EventListener callback);

    public static ConfigTool getConfigTool (Config config, EventListener callback) {
        if(GraphicsEnvironment.isHeadless())
            return new ConsoleConfigTool(config, callback);
        else
            return new GUIConfigTool(config, callback);
    }

    public interface EventListener  {
        public void onConfigComplete();
        public void onConfigFailed();
    }
}
