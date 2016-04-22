package it.simonedegiacomi.configuration;


import java.awt.*;

/**
 * Created on 27/12/2015.
 * @author Degiacomi Simone
 */
public abstract class LoginTool {

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
