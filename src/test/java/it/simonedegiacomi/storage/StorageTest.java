package it.simonedegiacomi.storage;


import com.sun.beans.util.Cache;
import it.simonedegiacomi.MyTestUtils;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.client.SyncEventListener;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test tests the storage using a StandardGBClient to emit event and try to catch them with a special
 * event emitter.
 * Created on 22/05/2016
 * @author Degiacomi Simone
 */
public class StorageTest {

    private String folder = "temp";
    private Storage storage;
    private EventEmitter eventEmitter;
    private StandardGBClient client;
    private CountDownLatch countDownLatch;

    @Before
    public void initStorage () throws IOException, StorageException, ClientException {
        Config.loadLoggerConfig();
        Config.getInstance().setProperty("path", folder);
        new File(folder).mkdir();

        // Create the storage
        storage = new Storage(MyTestUtils.loadAuth("storage_auth.properties"));
        eventEmitter = storage.getEnvironment().getEmitter();
        storage.getEnvironment().setSync(new Sync(storage.getInternalClient(), MyFileSystemWatcher.getDefault(folder)));
        storage.startStoraging();

        // Prepare the standard client
        client = new StandardGBClient(MyTestUtils.loadAuth("client_auth.properties"));
        client.init();
    }

    private void stop () {
        try {
            client.shutdown();
            storage.shutdown();
        } catch (ClientException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void createDirectory () throws ClientException {
        countDownLatch = new CountDownLatch(1);
        GBFile folderToCreate = new GBFile("prova", GBFile.ROOT_ID, true);
        SyncEvent expectedEvent = new SyncEvent(SyncEvent.EventKind.FILE_CREATED, folderToCreate);
        eventEmitter.setInternalListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent syncEvent) {
                assertEquals(expectedEvent, syncEvent);
                countDownLatch.countDown();
                stop();
            }
        });
        client.createDirectory(folderToCreate);
    }

    @Test
    public void createFile () throws ClientException {
        countDownLatch = new CountDownLatch(1);
        GBFile fileToCreate = new GBFile("prova.txt", GBFile.ROOT_ID, false);
        SyncEvent expectedEvent = new SyncEvent(SyncEvent.EventKind.FILE_CREATED, folderToCreate);
        eventEmitter.setInternalListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent syncEvent) {
                assertEquals(expectedEvent, syncEvent);
                countDownLatch.countDown();
                stop();
            }
        });
        client.uploadFile(fileToCreate);
    }

    @After
    public void end () throws InterruptedException {
        boolean completed = countDownLatch.await(800000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        if (!completed) {
            stop();
        }
        MyFileUtils.delete(new File(folder));
    }
}
