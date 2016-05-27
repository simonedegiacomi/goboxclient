package it.simonedegiacomi.storage.components.core;

import it.simonedegiacomi.IntegrationTest;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.ClientException;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class StorageToClientIT extends it.simonedegiacomi.storage.components.core.ComponentTest implements IntegrationTest {

    @Test
    public void receiveFirstAvailableFile () throws IOException, ClientException {
        countDownLatch = new CountDownLatch(1);

        GBFile rootFile = client.getInfo(GBFile.ROOT_FILE);

        GBFile toDownload = null;

        // Search for a file
        for (GBFile child : rootFile.getChildren()) {
            if (!child.isDirectory()) {
                toDownload = child;
                break;
            }
        }

        // I need at least one file
        assertNotNull(toDownload);

        FileOutputStream toFile = new FileOutputStream(new File("testDownload"));

        client.getFile(toDownload, toFile);

        new File("testDownload").delete();

        countDownLatch.countDown();
    }
}
