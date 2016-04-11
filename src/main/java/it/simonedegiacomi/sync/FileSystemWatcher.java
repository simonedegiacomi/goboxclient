package it.simonedegiacomi.sync;

import com.sun.nio.file.SensitivityWatchEventModifier;
import it.simonedegiacomi.goboxapi.GBFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple File System Watcher
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
 */
public class FileSystemWatcher extends Thread {

    /**
     * Kinds of event
     */
    public static final String FILE_CREATED = StandardWatchEventKinds.ENTRY_CREATE.name();
    public static final String FILE_DELETED = StandardWatchEventKinds.ENTRY_DELETE.name();
    public static final String FILE_CHANGED = StandardWatchEventKinds.ENTRY_MODIFY.name();

    /**
     * Java Watch service
     */
    private WatchService watchService;

    /**
     * Map of watch key, one for each watched folder
     */
    private HashMap<WatchKey, Path> keys = new HashMap<>();

    /**
     * Map of listeners for the events
     */
    private HashMap<String, Listener> listeners = new HashMap<>();

    /**
     * Set of files to ignore
     */
    private final Set<GBFile> filesToIgnore = new HashSet<>();

    /**
     * Rot to watch
     */
    private Path pathToWatch;

    /**
     * Count down latch used to shutdown this watcher
     */
    private CountDownLatch shutdownLatch = new CountDownLatch(2);

    /**
     * Create a new watcher and register the existing folder
     * @param path Path to watch
     * @throws IOException thrown watching this path
     */
    public FileSystemWatcher (Path path) throws IOException {
        pathToWatch = path;
        watchService = FileSystems.getDefault().newWatchService();
    }

    /**
     * Register (start watching) a new folder
     * @param path Path of the folder to watch
     * @throws IOException
     */
    private void registerFolder (Path path) throws IOException {
        /**
         * Walk trough the folder inside this folder
         */
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                // Register this folder
                WatchKey key = dir.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);

                // Save the watch key in the map
                keys.put(key, dir);

                // Continue the walk
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Assing a new event listener
     * @param eventName Kind of event
     * @param listener Listener of this event
     */
    public void assignListener (String eventName, Listener listener) {
        listeners.put(eventName, listener);
    }

    /**
     * Thread that pool the fs
     */
    @Override
    public void run () {
        // Register the already existing folder
        try {
            registerFolder(pathToWatch);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (shutdownLatch.getCount() > 1) {
            try {
                WatchKey watchKey = watchService.poll(500, TimeUnit.MICROSECONDS);
                if(watchKey == null)
                    continue;

                Path dir = keys.get(watchKey);

                for(WatchEvent<?> event : watchKey.pollEvents()) {

                    final WatchEvent.Kind kind = event.kind();

                    System.out.printf("KIND: %s ", kind.toString());

                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path name = ev.context();
                    Path changePath = dir.resolve(name);

                    final File file = changePath.toFile();

                    if(shouldIgnore(new GBFile(file)))
                        continue;
                    
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            listeners.get(kind.name()).onEvent(file);
                        }
                    }).start();

                    // If is a directory, start watch this
                    if (file.isDirectory() && kind == StandardWatchEventKinds.ENTRY_CREATE)
                        try{
                            registerFolder(changePath);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                }

                if(!watchKey.reset())
                    keys.remove(watchKey);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        try {
            watchService.close();
        } catch (IOException ex) {
            // TODO: handle this
        }
        shutdownLatch.countDown();
    }

    public void startIgnoring (GBFile file) {
        filesToIgnore.add(file);
    }

    public void stopIgnoring (GBFile file) { filesToIgnore.remove(file);}

    private boolean shouldIgnore (GBFile file) {

        // Get the path of the file
        List<GBFile> path = file.getPathAsList();

        // Iterate every file to ignore
        for (GBFile toIgnore : filesToIgnore) {

            // Get the path if the file to ignore
            List<GBFile> pathToIgnore = toIgnore.getPathAsList();

            boolean ignore = true;

            for(int i = 0;i < pathToIgnore.size() && i < path.size(); i++) {

                if (!pathToIgnore.get(i).equals(path.get(i))) {

                    ignore = false;
                    break;
                }
            }

            if (ignore)
                return true;
        }
        return false;
    }

    /**
     * Interface of the event listener
     */
    public interface Listener {
        public void onEvent (File file);
    }

    /**
     * Stop watching for new file system events
     * @throws InterruptedException
     */
    public void shutdown () throws InterruptedException{
        shutdownLatch.countDown();
        shutdownLatch.await();
    }
}