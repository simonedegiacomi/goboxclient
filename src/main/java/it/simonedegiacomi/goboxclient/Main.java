package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.GUIConnectionTool;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.StandardClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.utils.EasyProxy;
import it.simonedegiacomi.utils.SingleInstancer;
import it.simonedegiacomi.utils.Speaker;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This class load the config, let the user log in and
 * start the client, the sync and the storage
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
**/
public class Main {

    // Default delay to wait between the new connection attempt
    private static final int DEFAULT_RESTART_DELAY = 5000;

    private static final String DEFAULT_LOG_CONFIG = "config/log.conf";

    private static final Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    private static final Config config = Config.getInstance();

    private static TrayController tray;

    private static Speaker speaker;

    private static Storage storage;

    private static Sync sync;

    private static Client client;

    public static void main(String[] args) {

        // Configure the logger
        PropertyConfigurator.configure(DEFAULT_LOG_CONFIG);

        // Check if this is the only instance
        SingleInstancer singler = new SingleInstancer();

        // Otherwise close the program
        //if(!singler.isSingle())
        //    error("GoBox already running");

        // Set the shutdown action
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run () {
                shutdown();
            }
        });

        // Initialize controls
        initializeControls();

        // When the config change
        config.addOnconfigChangeListener(new Config.OnConfigChangeListener() {
            @Override
            public void onChange() {

                // Reload the proxy configuration
                EasyProxy.manageProxy(config);
                advice("Configuration reloaded");
            }
        });

        try {

            // Load the resources and initialize the classes
            initializeEnvironment();
        } catch (IOException ex) {

            error("Can't load resources files");
        }

        // If the user is not logged start the login wizard
        if (config.getProperty("username") == null) {

            advice("No Authentication token found. Start login procedure");

            // Start login
            startLogin();
        } else {

            // Login already done
            afterLogin();
        }
    }

    /**
     * Initialize controls like the icon tray
     */
    private static void initializeControls () {

        // If there is a graphic interface add the tray icon
        if(!GraphicsEnvironment.isHeadless()) {
            tray = new TrayController();

            // Set the connection settings button
            tray.setSettingsButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new GUIConnectionTool().show();
                }
            });

            // Set the close action
            tray.setOnCloseListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    shutdown();
                    System.exit(0);
                }
            });

            // Show the icon in the tray
            tray.showTray();
        }

        // Initialize speaker
        speaker = new Speaker(new Speaker.Listener() {
            @Override
            public void onMessage(String message) {
                advice(message);
            }
        });
    }

    /**
     * This method load the urls and the config
     * @throws IOException File not found or invalid
     */
    private static void initializeEnvironment () throws IOException {

        // Load the urls
        config.loadUrls();

        // Set the urls
        Auth.setUrlBuilder(config.getUrls());
        StandardClient.setUrlBuilder(config.getUrls());

        // Load the other config
        config.load();

        // Apply this config and reload the needed components with the new config
        config.apply();
    }

    /**
     * Start the login wizard
     */
    private static void startLogin() {

        // Get and start the right login tool
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
     * After the login, resync and start the client/storage
     */
    private static void afterLogin() {

        // Check if the auth token is still valid
        Auth auth = config.getAuth();

        try {

            // Try to authenticate
            advice("Authenticating...");

            if(!auth.check()) {

                error("Invalid authentication token", false);

                // If can't authenticate because the data is wrong, delete the auth information
                config.deleteAuth();

                // And save the configuration
                config.save();

                // Restart the login procedure
                startLogin();

                return;
            }

            // Save the configuration with the new token
            config.save();
        } catch (AuthException ex) {

            // If there was an error with the connection retry later
            disconnected();
            return;
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
        }
    }

    /**
     * Start the client mode
     * @param auth Auth object to use to instantiate the client
     */
    private static void startClientMode (Auth auth) {

        advice("Connecting as client ...");
        try {

            // Create the client
            client = new StandardClient(auth);

            // And the sync object
            sync = new Sync(client);

            // Set the speaker
            sync.setSpeaker(speaker);

            ((StandardClient) client).onDisconnect(new StandardClient.DisconnectedListener() {
                @Override
                public void onDisconnect() {

                    // Call the shutdown method
                    shutdown();

                    // And retry later
                    disconnected();
                }
            });

            // Connect the client to the server and the storage
            ((StandardClient) client).connect();

            // If there is the gui
            if (!GraphicsEnvironment.isHeadless()) {

                // Update the tray icon
                tray.setMode("Bridge");
            }

            try {

                // Try to switch to direct mode
                ((StandardClient) client).switchMode(StandardClient.ConnectionMode.DIRECT_MODE);

            } catch (ClientException ex) {

                logger.warn(ex);
            }

            // Start sync
            sync.resyncAndStart();

            advice("Ready");

            if (!GraphicsEnvironment.isHeadless()) {

                tray.setSyncCheckUsability(true);
            }

        } catch (ClientException ex) {

            disconnected();
        } catch (IOException ex) {

            disconnected();
        }
    }

    /**
     * Called when the client is disconnected
     */
    private static void disconnected () {

        advice("Connection Error");

        // wait some seconds...
        try {

            Thread.sleep(DEFAULT_RESTART_DELAY);
        } catch (InterruptedException ex) { }

        advice("New connection attempt");

        // Restart
        afterLogin();
    }

    /**
     * Called to start the storage
     * @param auth Auth object to use
     */
    private static void startStorageMode (Auth auth) {

        advice("Connecting as storage...");
        try {

            // Create the storage
            storage = new Storage(auth);

            // Create the sync object
            sync = new Sync(storage.getInternalClient());

            // Set the sync object to the storage environment
            storage.getEnvironment().setSync(sync);

            // Add a listener for the storage disconnection
            storage.onDisconnected(new Storage.DisconnectedListener() {
                @Override
                public void onDisconnected() {

                    shutdown();

                    // Call the function that will try to reconnect soon
                    disconnected();
                }
            });

            // Start event listener and http server
            storage.startStoraging();

            // Sync the folders and files
            sync.resyncAndStart();

            // If the Graphics Environment is available, add a tray icon
            if (!GraphicsEnvironment.isHeadless()) {

                // configure the tray icon
                tray.setSyncCheckUsability(false);
                tray.setMode("Storage");
            }
        } catch (StorageException e) {

            logger.warn(e);
            disconnected();
        } catch (ClientException e) {

            logger.warn(e);
            disconnected();
        } catch (IOException e) {

            logger.warn(e);
            disconnected();
        }

        advice("Ready");
    }

    /**
     * Print the error message in the console or untrash an error dialog. Then
     * exit the program
     * @param message Error message to untrash
     */
    private static void error (String message) {
        error(message, true);
    }

    private static void error (String message, boolean close) {
        logger.fatal(message);
        if (!GraphicsEnvironment.isHeadless())
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (close)
            System.exit(-1);
    }

    /**
     * Print in the console or untrash in the tray icon the message
     * @param message Message to untrash
     */
    private static void advice (String message) {
        logger.info(message);
        if (tray != null) {
            // Show in the icon tray
            tray.setMessage(message);
        }
    }

    /**
     * Prepare the program to exit correctly disconnection all the object
     */
    private static void shutdown () {

        if(storage != null) {
            storage.shutdown();
        }

        if(sync != null) {
            try {
                sync.shutdown();
            } catch (Exception ex) { }
        }

        if(client != null) {
            try {
                client.shutdown();
            } catch (Exception ex) { }
        }
    }
}