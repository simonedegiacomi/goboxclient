package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.StandardClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.utils.EasyProxy;
import it.simonedegiacomi.utils.SingleInstancer;
import javafx.scene.control.Alert;

import java.awt.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Degiacomi Simone on 24/12/2015.
**/
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static Config config = Config.getInstance();

    private static Sync sync;

    public static void main(String[] args) {

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        Logger.getAnonymousLogger().addHandler(consoleHandler);


        SingleInstancer singler = new SingleInstancer();
        // Close the program
        // TODO: Add a message or open the configuration of GoBox
        //if(!singler.isSingle())
        //    System.exit(0);
        System.out.println("Single instance? " + singler.isSingle());

        config.addOnconfigChangeListener(new Config.OnConfigChangeListener() {
            @Override
            public void onChange() {

                // Check if there is a proxy to use
                EasyProxy.manageProxy(config);
            }
        });

        try {
            // Load the urls to use to contact the server
            config.loadUrls();
            // Try to load the config
            config.load();

            if (config.getProperty("username") == null)
                startLogin();
            else
                afterLogin();
        } catch (Exception ex) {

            log.warning("No config file found");
            // If something fails, it means that there is no config
            // file, so let's create a new config
            startLogin();
        }
    }

    private static void startLogin() {
        LoginTool.getLoginTool(new LoginTool.EventListener() {
            @Override
            public void onLoginComplete() {
                // initialize some variables in the config
                config.setProperty("path", "files");
                afterLogin();
            }

            @Override
            public void onLoginFailed() {
                log.warning("Configuration failed");
                showError();
                System.exit(-1);
            }
        });
    }

    private static void afterLogin() {

        // try the token
        Auth auth = config.getAuth();
        try {
            log.fine("Checking identity");
            auth.check();
            config.save();
            log.fine("Authenticated");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(config.getMode());

        switch (auth.getMode()) {
            case CLIENT_MODE:
                startClientMode(auth);
                break;
            case STORAGE_MODE:
                startStorageMode(auth);
                break;
        }

        // If there is a graphic interface add the tray icon
        if(!GraphicsEnvironment.isHeadless()) {
            TrayController tray = new TrayController(sync);
            tray.showTray();
        }
    }

    private static void startClientMode (Auth auth) {
        try {
            Client client = new StandardClient(auth);
            sync = new Sync(client);

            sync.receiveNotHeardEvent();

            sync.startSync();

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    private static void startStorageMode (Auth auth) {
        // Conect to the database
        try {
            // start event listener and http server
            Storage storage = new Storage(auth);

            storage.startStoraging();

            // start Sync
            sync = new Sync(storage.getInternalClient());

            sync.startSync();

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