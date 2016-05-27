package it.simonedegiacomi.storage.components.core;

import it.simonedegiacomi.IntegrationTest;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.ClientException;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertNotNull;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class FileInfoIT extends it.simonedegiacomi.storage.components.core.ComponentTest implements IntegrationTest {

    @Test
    public void getRootInfo () throws IOException, ClientException {
        countDownLatch = new CountDownLatch(1);

        GBFile detailedRoot = client.getInfo(GBFile.ROOT_FILE);
        assertNotNull(detailedRoot);

        countDownLatch.countDown();
    }
}
