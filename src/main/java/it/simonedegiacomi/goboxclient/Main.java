package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.StandardClient;
import it.simonedegiacomi.goboxclient.ui.*;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.utils.GoBoxInstance;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;

/**
 * This class load the config, let the user log in and start the client, the sync and the storage
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
**/
public class Main {

    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * Default delay to wait between the new connection attempt
     */
    private static final int DEFAULT_RESTART_DELAY = 5000;

    /**
     * Config instance
     */
    private static final Config config = Config.getInstance();

    /**
     * Facade instance of this program (a sort of a control panel)
     */
    private static final GoBoxFacade facade = new GoBoxFacade();

    /**
     * Model instance of the program
     */
    private final static Model goboxModel = new GoBoxModel(facade);

    private final static Presenter presenter = new GoBoxPresenter(goboxModel);

    public static void main(String[] args) {
        try {
            facade.initializeEnvironment();
        } catch (IOException ex) {
            logger.warn("Environment not initialized correctly.");
        }

        // Check if this is the only instance
        GoBoxInstance instance = new GoBoxInstance();

        // Otherwise start the CLI interface
        if(!instance.isSingle()) {
            try {
                instance.sendToMainInstance(args);
            } catch (IOException ex) {
                System.out.println("GoBox CLI is not available");
            }
            return;
        }

        // Set the presenter to this instance
        instance.setPresenter(presenter);

        // If there is a gui, create the tray view
        if (!GraphicsEnvironment.isHeadless()) {
            TrayView view = new TrayView(presenter);
            presenter.addView(view);
        }

        // If the user is not logged (or if the configuration was not loaded) start the login wizard
        if (config.isAuthDefined()) {
            afterLogin();
        } else {
            startLogin();
        }
    }

    /**
     * Start the login wizard
     */
    private static void startLogin() {

        // Get and start the right login tool
        LoginTool.startLogin(() -> {
            try {
                config.save();
            } catch (IOException ex) {
                logger.warn("Cannot save auth credentials");
            }
            afterLogin();
        }, () -> error("Login failed. Please restart GoBoxClient"));
    }

    /**
     * After the login, resync and start the client/storage
     */
    private static void afterLogin() {

        // Check if the auth token is still valid
        Auth auth = config.getAuth();

        try {

            // Try to authenticate
            goboxModel.setFlashMessage("Authenticating...");

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
            logger.warn(ex);
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
        goboxModel.setFlashMessage("Connecting as client ...");

        // Create the client
        StandardClient client = new StandardClient(auth);
        facade.setClient(client);

        try {

            // Create the sync object
            Sync sync = new Sync(client);
            facade.setSync(sync);

            client.onDisconnect(new StandardClient.DisconnectedListener() {
                @Override
                public void onDisconnect() {

                    // Call the shutdown method
                    facade.shutdown();

                    // And retry later
                    disconnected();
                }
            });

            // Connect the client to the server and the storage
            client.init();

            try {

                // Try to switch to direct mode
                client.switchMode(StandardClient.ConnectionMode.DIRECT_MODE);

            } catch (ClientException ex) {
                logger.warn(ex);
            }

            // Start sync
            sync.resyncAndStart();

            goboxModel.setFlashMessage("Ready");

        } catch (ClientException ex) {
            logger.warn(ex);
            disconnected();
        } catch (IOException ex) {
            logger.warn(ex);
            disconnected();
        }
    }

    /**
     * Called when the client is disconnected
     */
    private static void disconnected () {

        // Shutdown all the object
        facade.shutdown();

        // Advise the user
        goboxModel.setFlashMessage("Disconnected. Retry soon...");

        try {
            // wait some seconds...
            Thread.sleep(DEFAULT_RESTART_DELAY);
        } catch (InterruptedException ex) { }

        goboxModel.setFlashMessage("New connection attempt");

        // Restart
        afterLogin();
    }

    /**
     * Called to start the storage
     * @param auth Auth object to use
     */
    private static void startStorageMode (Auth auth) {
        goboxModel.setFlashMessage("Connecting as storage...");

        try {

            // Create the storage
            Storage storage = new Storage(auth);
            facade.setStorage(storage);
            facade.setClient(storage.getInternalClient());

            // Create the sync object
            Sync sync = new Sync(storage.getInternalClient());
            facade.setSync(sync);

            // Set the sync object to the storage environment
            storage.getEnvironment().setSync(sync);

            // Add a listener for the storage disconnection
            storage.onDisconnected(new Storage.DisconnectedListener() {
                @Override
                public void onDisconnected() {
                    facade.shutdown();

                    // Call the function that will try to reconnect soon
                    disconnected();
                }
            });

            // Start event listener and http server
            storage.startStoraging();

            // Sync the folders and files
            sync.resyncAndStart();
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

        goboxModel.setFlashMessage("Ready");
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
        goboxModel.setError(message);

        if (close)
            facade.exit();
    }
}