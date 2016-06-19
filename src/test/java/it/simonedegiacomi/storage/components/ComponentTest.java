package it.simonedegiacomi.storage.components.core;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.authentication.PropertiesAuthLoader;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.storage.Storage;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public abstract class ComponentTest {

    protected CountDownLatch countDownLatch;

    protected StorageEnvironment env;

    protected Storage storage;

    protected GBClient client;

    @Before
    public void init () throws IOException, SQLException, StorageException, ClientException {
        org.apache.log4j.BasicConfigurator.configure();

//        GBAuth storageAuth = PropertiesAuthLoader.loadAndLoginFromFile(new File(getClass().getResource("/storage_auth.properties").getFile()));
//        Config.getInstance().setAuth(storageAuth);
//        env = new StorageEnvironment();
//        storage = new Storage(storageAuth, env);
//        storage.startStoraging();
//
//        client = new StandardGBClient(PropertiesAuthLoader.loadAndLoginFromFile(new File(getClass().getResource("/client_auth.properties").getFile())));
//        client.init();
    }


    @After
    public void end () throws InterruptedException, ClientException {
        assertTrue(countDownLatch.await(5000, TimeUnit.MILLISECONDS));
        client.shutdown();
        storage.shutdown();
    }
}
