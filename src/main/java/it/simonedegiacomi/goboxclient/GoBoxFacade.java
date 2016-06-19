package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.LoginTool;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.InternalClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.Work;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import it.simonedegiacomi.utils.EasyProxy;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * This facade class is some sort of control panel of the program instance. From here you
 * can get the status of the program instance, stop it and set other settings.
 * <p>
 * TODO: Some method are used like if this is the GoBoxEnvironment. Restructure code
 * <p>
 * Created on 20/04/16.
 *
 * @author Degiacomi Simone
 */
public class GoBoxFacade {

    /**
     * Logger of the class
     */
    private static final Logger logger = Logger.getLogger(GoBoxFacade.class);

    /**
     * Configuration
     */
    private final Config config = Config.getInstance();

    /**
     * Environment
     */
    private final GoBoxEnvironment env = new GoBoxEnvironment();

    /**
     * Create a new gobox facade object, and add a listener to the config
     */
    public GoBoxFacade() {

        // Set the default log config
        BasicConfigurator.configure();

        // When the configuration changes reload the proxy
        config.addOnconfigChangeListener(() -> EasyProxy.handleProxy(config));
    }

    /**
     * Shutdown the sync, the client and the storage (if was running)
     */
    public void shutdown() {
        env.shutdown();
    }

    /**
     * Call shutdown and then exit the program
     */
    public void exit() {
        shutdown();
        System.exit(0);
    }

    /**
     * This method load the urls and the config
     *
     * @throws IOException File not found or invalid
     */
    public void initialize(File configFile) throws IOException {

        if (configFile.exists()) {
            config.load(configFile);
        }

        // Load the urls
        URLBuilder.DEFAULT.init();

        // Apply this config and reload the needed components with the new config
        config.apply();
    }

    public void start() {

        // Assert that the user is logged
        assertLogged();

        // Find the path to watch
        File pathToWatch = config.getFolder("path", "files/");

        try {
            // Now create the FileSystemWatcher
            MyFileSystemWatcher watcher = MyFileSystemWatcher.getDefault(pathToWatch.getAbsolutePath());
            env.setFileSystemWatcher(watcher);
        } catch (IOException ex) {
            logger.warn("fileSystemWatcher initialization failed", ex);
            System.exit(-1);
        }

        // Prepare the client
        switch (env.getAuth().getMode()) {
            case STORAGE:
                prepareStorage();
                break;
            case CLIENT:
                prepareClient();
                swicthBestMode();
                break;
        }

        // create Sync object
        Sync sync = new Sync(env);
        env.setSync(sync);

        try {
            // Start syncing
            sync.syncAndStart();
        } catch (ClientException | IOException ex) {
            logger.warn("Synchronization failed", ex);
        }
    }

    /**
     * Assert that the user is logged.
     */
    private void assertLogged() {
        try {
            if (env.getAuth().check()) {
                logger.info("User successfully logged in");
                return;
            }
        } catch (IOException ex) {
            logger.warn("Can't communicate with th server to perform the authentication", ex);
            System.exit(-1);
        }

        // Here the user is not logged, so try to login

        // Login is asynchronous, so use a CountDownLatch
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // Start the login
        LoginTool.startLogin(() -> {
            logger.info("Authentication procedure completed");
            countDownLatch.countDown();
        }, () -> {
            logger.warn("Authentication procedure failed");
            System.exit(-1);
        });

        // Wait for the login procedure
        try {
            countDownLatch.await();
        } catch (InterruptedException ex) {
            logger.warn(ex.toString(), ex);
            System.exit(-1);
        }
    }

    /**
     * Prepare the storage
     */
    private void prepareStorage() {
        try {
            // Create the storage
            Storage storage = new Storage(env);

            // Set disconnection listener
            storage.onDisconnected(() -> {
                storage.shutdown();
                restart();
            });

            // Start it
            storage.startStoraging();

            // Get the InternalClient
            env.setClient(storage.getEnvironment().getClient());
        } catch (SQLException ex) {
            logger.warn("Storage database initialization failed", ex);
            System.exit(-1);
        } catch (StorageException ex) {
            logger.warn("Storage initialization failed", ex);
            System.exit(-1);
        }
    }

    /**
     * Prepare the client
     */
    private void prepareClient() {
        // Create the StandardGBClient
        StandardGBClient client = new StandardGBClient(env.getAuth());
        env.setClient(client);

        // Set the disconnection listener
        client.onDisconnect(() -> restart());

        try {
            // Start the client
            if (!client.init()) {
                logger.warn("Client initialization failed");
                System.exit(-1);
            }
        } catch (ClientException ex) {
            logger.warn("Client initialization failed");
            System.exit(-1);
        }
    }

    /**
     * Return the authentication mode (client or storage)
     *
     * @return Current authentication mode
     */
    public GBAuth.Modality getAuthMode() {
        return env.getAuth().getMode();
    }

    /**
     * Switch the connection mode
     *
     * @param nextMode Next connection mode
     * @throws ClientException Exception thrown while switching mode
     */
    public void switchClientMode(StandardGBClient.ConnectionMode nextMode) throws ClientException {
        if (!(env.getClient() instanceof StandardGBClient)) {
            throw new IllegalStateException("Current client is not a StandardGBClient");
        }

        // Just call the method
        ((StandardGBClient) env.getClient()).switchMode(nextMode);
    }

    /**
     * Shutdown and restart
     */
    public void restart() {
        shutdown();
        start();
    }

    public GoBoxEnvironment getEnvironment() {
        return env;
    }

    private void swicthBestMode() {
        // Switch to the best mode
        StandardGBClient.ConnectionMode[] connectionModes = {StandardGBClient.ConnectionMode.LOCAL_DIRECT_MODE, StandardGBClient.ConnectionMode.DIRECT_MODE};
        for (StandardGBClient.ConnectionMode mode : connectionModes) {
            try {
                switchClientMode(mode);
                break;
            } catch (ClientException ex) {

            }
        }
    }
}