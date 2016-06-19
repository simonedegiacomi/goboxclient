package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.goboxclient.ui.*;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import it.simonedegiacomi.utils.GoBoxInstance;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This class load the config, let the user log in and start the client, the sync and the storage
 * <p>
 * Created on 24/12/2015.
 *
 * @author Degiacomi Simone
 **/
public class Main {

    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * Facade instance of this program (a sort of a control panel)
     */
    private static final GoBoxFacade facade = new GoBoxFacade();

    private static Presenter presenter;

    private static final GoBoxModel model = new GoBoxModel();

    public static void main(String[] args) {

        // Prepare the user interface
        prepareUI();

        // Load configuration file
        loadConfig(args);

        // Register shutdown listener
        registerShutdown();

        // Handle the case when this is not the first instance
        handleSingleInstance(args);

        // Start the client program
        facade.start();
    }

    /**
     * Load the configuration
     * @param args CLI arguments
     */
    private static void loadConfig (String[] args) {
        try {
            facade.initialize(new File(args.length == 1 ? args[1] : Config.DEFAULT_CONFIG_FILE));
        } catch (IOException ex) {
            logger.warn("Environment not initialized correctly.", ex);
            model.setError("Environment initialization Failed");
            System.exit(-1);
        }
    }

    /**
     * Register the shutdown listener
     */
    private static void registerShutdown () {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                facade.shutdown();
            }
        });
    }

    /**
     * Prepare the UI object
     */
    private static void prepareUI() {

        // Create the model
        facade.getEnvironment().setModel(model);

        // Create the presenter
        presenter = new GoBoxPresenter(facade.getEnvironment());

        // If there is a gui, create the TrayView
        if (!GraphicsEnvironment.isHeadless()) {
            TrayView view = new TrayView(presenter);
            presenter.addView(view);
        }

        // Add the log view
        presenter.addView(new LogView());
    }

    private static void handleSingleInstance (String[] args) {

        // Check if this is the only instance
        GoBoxInstance instance = new GoBoxInstance();

        // Otherwise start the CLI interface
        if(!instance.isSingle()) {
            try {
                instance.sendToMainInstance(args);
            } catch (IOException ex) {
                logger.warn("GoBox CLI is not available", ex);
                System.exit(-1);
            }
            System.exit(0);
        }

        // Set the presenter to this instance
        instance.setPresenter(presenter);
    }
}