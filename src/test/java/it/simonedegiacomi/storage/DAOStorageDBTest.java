package it.simonedegiacomi.storage;

import it.simonedegiacomi.goboxapi.GBFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * Created on 27/04/16.
 * @author Degiacomi Simone
 */
public class DAOStorageDBTest {

    private StorageDB storageDB;

    @Before
    public void setUp() throws StorageException {
        //storageDB = new DAOStorageDB("./config/testdb");
    }

    @After
    public void tearDown() throws IOException, SQLException {
        storageDB.close();
        new File("./config/testdb.mv.db").delete();
    }

    @Test
    public void insertFile () throws StorageException {
        GBFile file = new GBFile("file", GBFile.ROOT_ID, false);
        storageDB.insertFile(file);

        GBFile dbFile = storageDB.getFile(file);
        assertEquals(file, dbFile);

        GBFile folder = new GBFile("folder", GBFile.ROOT_ID, true);
        storageDB.insertFile(folder);
        dbFile = storageDB.getFile(folder);
        assertEquals(folder, dbFile);
    }

    @Test
    public void childTest () throws StorageException, SQLException {


        GBFile folder = new GBFile("documents", GBFile.ROOT_ID, true);

        storageDB.insertFile(folder);
        GBFile dbFolder = storageDB.getFileByID(folder.getID());

        assertEquals(folder, dbFolder);

        GBFile pdfFodler = folder.generateChild("pdf", true);
        storageDB.insertFile(pdfFodler);

        GBFile pdf = pdfFodler.generateChild("file.pdf", false);
        storageDB.insertFile(pdf);

    }
}