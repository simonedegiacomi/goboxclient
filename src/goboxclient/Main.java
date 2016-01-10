package goboxclient;

import configuration.Config;
import configuration.ConfigTool;
import goboxapi.authentication.Auth;
import goboxapi.client.Client;
import goboxapi.client.StandardClient;
import javafx.scene.control.Alert;
import storage.Storage;
import utils.SingleInstancer;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Degiacomi Simone on 24/12/2015.
**/
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static Config config = Config.getInstance();

    public static void main(String[] args) {
        SingleInstancer singler = new SingleInstancer();
        // Close the program
        // TODO: Add a message or open the configuration of GoBox
        if(!singler.isSingle())
            System.exit(0);

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
                    // initialize some variables in the config
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

        // If there is a graphic interface add the tray icon
        if(!GraphicsEnvironment.isHeadless())
            addIconTray();
    }

    private static void startClientMode (Auth auth) {
        try {
            Client client = new StandardClient(auth);
            Sync sync = new Sync(client);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    private static void startStorageMode (Auth auth) {
        // Conect to the database
        try {
            // start event listener and http server
            Storage storage = new Storage(auth);

            // start Sync
            Sync sync = new Sync(storage.getInternalClient());

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

    private static void addIconTray() {

    }
}