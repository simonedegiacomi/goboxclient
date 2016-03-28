package it.simonedegiacomi.configuration;


import java.awt.*;

/**
 * Created by Degiacomi Simone onEvent 27/12/2015.
 */
public abstract class LoginTool {

    //public LoginTool(Config config, EventListener callback);

    public static LoginTool getLoginTool(EventListener callback) {
        if(GraphicsEnvironment.isHeadless())
            return new ConsoleLoginTool(callback);
        else
            return new GUILoginTool(callback);
    }

    public interface EventListener  {
        public void onLoginComplete();
        public void onLoginFailed();
    }
}
