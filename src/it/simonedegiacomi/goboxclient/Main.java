package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.StandardClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.utils.EasyProxy;
import it.simonedegiacomi.utils.SingleInstancer;
import it.simonedegiacomi.utils.Speaker;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class load the config, let the user log in and
 * start the client, the sync and the storage
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
**/
public class Main {

    private static final int DEFAULT_RESTART_DELAY = 5000;

    private static final Config config = Config.getInstance();

    private static TrayController tray;

    private static Speaker speaker;

    public static void main(String[] args) {

        // Get the console handler
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        Logger.getAnonymousLogger().addHandler(consoleHandler);

        // Check if this is the only instance
        SingleInstancer singler = new SingleInstancer();
        // Close the program
        if(!singler.isSingle())
            error("GoBox already running");

        // When the config change
        config.addOnconfigChangeListener(new Config.OnConfigChangeListener() {
            @Override
            public void onChange() {

                // Reload the proxy configuration
                EasyProxy.manageProxy(config);
            }
        });

        // Load the resources and initialize the classes
        try {
            initializeEnviroment();
        } catch (IOException ex) {
            error("Can't load resources files");
        }

        // If there is a graphic interface add the tray icon
        if(!GraphicsEnvironment.isHeadless()) {
            tray = new TrayController();
            tray.showTray();
        }

        // Initialize speaker
        speaker = new Speaker(new Speaker.Listener() {
            @Override
            public void onMessage(String message) {
                advice(message);
            }
        });

        // If the user is not logged start the login wizard
        if (config.getProperty("username") == null)
            startLogin();
        else
            afterLogin();
    }

    /**
     * This method load the urls and the config
     * @throws IOException File not found or invalid
     */
    private static void initializeEnviroment () throws IOException {
        // Load the urls
        config.loadUrls();

        // Set the urls
        Auth.setUrlBuilder(config.getUrls());
        StandardClient.setUrlBuilder(config.getUrls());
        Storage.setUrls(config.getUrls());

        // Load the other config
        config.load();

        // Apply this config and reload the needed components with the new config
        config.apply();
    }

    /**
     * Start the login wizard
     */
    private static void startLogin() {
        LoginTool.getLoginTool(new LoginTool.EventListener() {
            @Override
            public void onLoginComplete() {
                afterLogin();
            }

            @Override
            public void onLoginFailed() {
                error("Login failed");
            }
        });
    }

    /**
     * After the login resync and start the client/storage
     */
    private static void afterLogin() {

        // Check if the auth token is still valid
        Auth auth = config.getAuth();
        try {
            advice("Authenticating...");
            if(!auth.check()) {
                // If can't authenticate because the data is wrong, delete
                config.deleteAuth();
                config.save();
                error("Invalid authentication token");
            }
            config.save();
        } catch (AuthException ex) {
            // If there was an error just close the program
            ex.printStackTrace();
            error("Cannot authenticate");
        } catch (IOException ex) {
            error("Cannot save config file");
        }

        // Start the right client
        switch (auth.getMode()) {
            case CLIENT:
                startClientMode(auth);
                break;
            case STORAGE:
                startStorageMode(auth);
                break;
            //case MULTIPLE_STORAGE:
            //    break;
        }
    }

    /**
     * Start the client mode
     * @param auth Auth object to use to instantiate the client
     */
    private static void startClientMode (Auth auth) {
        advice("Connecting...");
        try {
            StandardClient client = new StandardClient(auth);
            Sync sync = new Sync(client);
            sync.setSpeaker(speaker);
            client.onDisconnect(new StandardClient.DisconnectedListener() {
                @Override
                public void onDisconnect() {
                    client.shutdown();
                    try {
                        sync.shutdown();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    disconnected();
                }
            });

            // Connect the client to the server and the storage
            client.connect();

            // Start sync
            sync.resyncAndStart();
        } catch (ClientException ex) {
            disconnected();
        } catch (IOException ex) {
            disconnected();
        }
        advice("Ready");
    }

    /**
     * Called when the client is disconnected
     */
    private static void disconnected () {
        advice("Connection Error");
        // wait some seconds...
        try {
            Thread.sleep(DEFAULT_RESTART_DELAY);
        } catch (InterruptedException ex) {
            // no problem
        }

        // Restart
        afterLogin();
    }

    /**
     * Called to start the storage
     * @param auth Auth object to use
     */
    private static void startStorageMode (Auth auth) {
        advice("Connecting");
        try {
            Storage storage = new Storage(auth);
            Sync sync = new Sync(storage.getInternalClient());
            storage.onDisconnected(new Storage.DisconnectedListener() {
                @Override
                public void onDisconnected() {
                    storage.shutdown();
                    try {
                        sync.shutdown();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    disconnected();
                }
            });

            // start event listener and http server
            storage.startStoraging();

            sync.resyncAndStart();
        } catch (Exception ex) {
            disconnected();
        }
        advice("Ready");
    }

    /**
     * Print the error message in the console or show an error dialog. Then
     * exit the program
     * @param message Error message to show
     */
    private static void error (String message) {
        if (GraphicsEnvironment.isHeadless())
            System.out.println(message);
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("GoBoxClient cannot be initialized");
            alert.setContentText(message);
            alert.showAndWait();
        }
        System.exit(-1);
    }

    /**
     * Print in the console or show in the tray icon the message
     * @param message Message to show
     */
    private static void advice (String message) {
        System.out.println(message);
        if (tray != null) {
            // Show in the icon tray
            tray.setMessage(message);
        }
    }
}