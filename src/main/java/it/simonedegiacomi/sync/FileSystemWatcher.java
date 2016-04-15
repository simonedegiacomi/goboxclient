package it.simonedegiacomi.sync;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
     * Map of files to ignore
     */
    private final Map<String, Long> filesToIgnore = new HashMap<>();

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
    public void addListener(String eventName, Listener listener) {
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

                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path name = ev.context();
                    Path changePath = dir.resolve(name);

                    final File file = changePath.toFile();
                    System.out.println("WATCHER PATH" + file.getPath());

                    if(shouldIgnore(file))
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

    /**
     * Ignore all the events relative to this file. If this file is a folder, all the children
     * events will be ignored
     * @param file File to ignore
     */
    public void startIgnoring (File file) {
        filesToIgnore.put(file.getPath(), -1L);
    }

    /**
     * Stop ignoring a file and receive new events. If this file is a folder, this is applied also
     * to his children.
     * NOTE that the file must be equivalent to the file that was passed as argument to the
     * {@link #startIgnoring(File) startIgnoring} method.
     * @param file File to stop ignoring
     */
    public void stopIgnoring (File file) {
        filesToIgnore.put(file.getPath(), System.currentTimeMillis());
    }

    /**
     * Check if a event relative to this file should be ignored or not
     * @param file File to check
     * @return Ignore or not
     */
    private boolean shouldIgnore (File file) {

        // Get the path of the file
        String[] path = file.getPath().split("/");

        // Iterate every file to ignore
        for (Map.Entry<String, Long> toIgnore : filesToIgnore.entrySet()) {

            // Get the path if the file to ignore
            String[] pathToIgnore = toIgnore.getKey().split("/");

            // Default ignore the file
            boolean ignore = true;

            // Iterate each piece
            for(int i = 0;i < pathToIgnore.length && i < path.length; i++) {

                // If a piece is different
                if (!pathToIgnore[i].equals(path[i])) {

                    // Ignore this file check, go to the next
                    ignore = false;
                    break;
                }
            }

            // If the file is present in the map, check if should continue to ignore
            if (ignore) {

                // Get the time
                long time = toIgnore.getValue();

                // If the file was ignored before the file event, ignore it
                if (time <= 0 || time >= file.lastModified()) {
                    return true;
                } else {

                    // Otherwise remove the file from the map
                    filesToIgnore.remove(toIgnore);
                }
            }
        }

        // Don't ignore the file
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