package webstorage;

import configuration.*;
import goboxapi.authentication.Auth;
import goboxapi.authentication.AuthException;
import goboxapi.client.Client;
import goboxapi.client.StandardClient;
import goboxclient.Sync;
import javafx.scene.control.Alert;
import storage.*;

import java.awt.*;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static Config config = Config.getInstance();

    public static void main(String[] args) {
        try {
            // Try to load the config
            config.load();
            afterConfigLoaded();
        } catch (Exception ex) {
            log.fine("No config file found");
            // If something fails, it means that there is no config
            // file, so let's create a new config
            ConfigTool tool = ConfigTool.getConfigTool(config, new ConfigTool.EventListener() {
                @Override
                public void onConfigComplete() {
                    // initialize some variables in the coonfig
                    config.setProperty("path", "files/");
                    afterConfigLoaded();
                }

                @Override
                public void onConfigFailed() {
                    log.warning("Configuration failed");
                    showError();
                    System.exit(-1);
                }
            });
        }
    }

    private static void afterConfigLoaded() {

        // try the token
        Auth auth = new Auth(config.getProperty("username"));
        auth.setToken(config.getProperty("token"));
        auth.setMode(Integer.parseInt(config.getProperty("mode")));
        try {
            System.out.println("Checking identity");
            auth.check();
            config.setProperty("token", auth.getToken());
            config.save();
            System.out.println("Done");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        switch (config.getMode()) {
            case Config.CLIENT_MODE:
                startClientMode(auth);
                break;
            case Config.STORAGE_MODE:
                startStorageMode(auth);
                break;
        }
    }

    private static void startClientMode (Auth auth) {
        StandardClient client = new StandardClient(auth);
        Sync sync = new Sync(client);
    }

    private static void startStorageMode (Auth auth) {
        // Conect to the database
        try {
            // start event listener and http server
            Storage storage = new Storage(auth);
            System.out.println("Storage created");
            // start Sync
            //Sync sync = new Sync(storage.getInternalClient());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }


    private static void showError() {
        if (GraphicsEnvironment.isHeadless())
            System.out.println("Installation aborted");
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Can't create the configuration");
            alert.setContentText("GoBoxClient cannot be initialized.");
            alert.showAndWait();
        }
    }
}