package it.simonedegiacomi.sync.fs;

import com.google.common.io.Files;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MyFileSystemWatcherTest {

    private static final String folder = "temp/";

    private MyFileSystemWatcher watcher;

    private CountDownLatch latch;

    private void stop () {
        try {
            watcher.shutdown();
        } catch (Exception ex) {
            fail();
        }
    }

    @Before
    public void initWatcher () throws IOException {
        new File(folder).mkdir();
        watcher = new JavaFileSystemWatcher(folder);
        //watcher = new JNotifyFileSystemWatcher(folder);
        latch = new CountDownLatch(1);
    }

    @Test
    public void createFile () throws IOException {
        File testFile = new File(folder + "prova.txt");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();
                stop();
            }

            @Override
            public void onFileModified(File modifiedFile) {
                fail();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                fail();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                fail();
            }
        });

        Files.touch(testFile);
    }

    @Test
    public void createFolder () {
        File testFile = new File(folder + "prova");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();
                stop();
            }

            @Override
            public void onFileModified(File modifiedFile) {
                fail();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                fail();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                fail();
            }
        });

        testFile.mkdir();
    }

    @Test
    public void deleteFile () throws IOException {
        latch = new CountDownLatch(2);

        File testFile = new File(folder + "prova.txt");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();
                testFile.delete();
            }

            @Override
            public void onFileModified(File modifiedFile) {
                fail();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                assertEquals(testFile, deletedFile);
                latch.countDown();
                stop();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                fail();
            }
        });

        Files.touch(testFile);

    }

    @Test
    public void deleteFolder () throws IOException {
        latch = new CountDownLatch(2);

        File testFile = new File(folder + "prova/");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();
                testFile.delete();
            }

            @Override
            public void onFileModified(File modifiedFile) {
                fail();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                assertEquals(testFile, deletedFile);
                latch.countDown();
                stop();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                fail();
            }
        });

        testFile.mkdir();

    }

    @Test
    public void modifyFile () throws IOException {
        latch = new CountDownLatch(2);

        File testFile = new File(folder + "prova.txt");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();

                // Now modify
                try {
                    PrintWriter toFile = new PrintWriter(new FileWriter(testFile));
                    toFile.print(System.currentTimeMillis());
                    toFile.close();
                } catch (Exception ex) {
                    fail();
                }
            }

            @Override
            public void onFileModified(File modifiedFile) {
                assertEquals(testFile, modifiedFile);
                latch.countDown();
                stop();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                fail();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                fail();
            }
        });

        Files.touch(testFile);
    }

    @Test
    public void moveFile () throws IOException {
        latch = new CountDownLatch(2);

        File testFile = new File(folder + "prova.txt");
        File moveFile = new File(folder + "prova2.txt");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                assertEquals(testFile, newFile);
                latch.countDown();

                // Now modify
                try {
                    Files.move(newFile, moveFile);
                } catch (Exception ex) {
                    fail();
                }
            }

            @Override
            public void onFileModified(File modifiedFile) {
                fail();
            }

            @Override
            public void onFileDeleted(File deletedFile) {
                fail();
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                assertEquals(moveFile, movedFile);
                System.out.print("File moved!");
                latch.countDown();
                stop();
            }
        });

        Files.touch(testFile);
    }

    @Test
    public void moveFolder () throws IOException {
        latch = new CountDownLatch(2);

        File testFile = new File(folder + "prova");
        File moveFile = new File(folder + "prova2");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                if (newFile.toString().equals(moveFile.toString()))
                    return;
                assertEquals(testFile, newFile);
                latch.countDown();

                // Now modify
                try {
                    Files.move(newFile, moveFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }
            }

            @Override
            public void onFileModified(File modifiedFile) {
                assertEquals(moveFile, modifiedFile);
            }

            @Override
            public void onFileDeleted(File deletedFile) {

                assertEquals(testFile, deletedFile);
            }

            @Override
            public void onFileMoved(File before, File movedFile) {
                assertEquals(moveFile, movedFile);
                latch.countDown();
                stop();
            }
        });

        testFile.mkdir();
    }

    @Test
    public void createFileInsideNewFolder () {
        latch = new CountDownLatch(2);

        File f = new File(folder + "cartella");
        File file = new File(f.toString() + "/file.txt");

        watcher.addListener(new MyFileSystemWatcher.FileSystemEventListener() {
            @Override
            public void onFileCreated(File newFile) {
                if (newFile.equals(file)) {
                    latch.countDown();
                    return;
                }
                assertEquals(f, newFile);
                try {
                    Files.touch(file);
                    System.out.println("File inside folder created " + file);
                } catch (IOException e) {
                    fail();
                }
                latch.countDown();
            }

            @Override
            public void onFileModified(File modifiedFile) {

            }

            @Override
            public void onFileDeleted(File deletedFile) {

            }

            @Override
            public void onFileMoved(File before, File movedFile) {

            }
        });
        f.mkdir();
    }

    @After
    public void clear () throws InterruptedException {
        boolean completed = latch.await(8000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        if (!completed) {
            stop();
        }
        MyFileUtils.delete(new File(folder));
    }
}