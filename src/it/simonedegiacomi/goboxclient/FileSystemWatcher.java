package it.simonedegiacomi.goboxclient;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Simple File System Watcher
 *
 * Created by Degiacomi Simone on 24/12/2015.
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
    private HashMap<WatchKey, Path> keys;

    /**
     * Map of listeners for the events
     */
    private HashMap<String, Listener> listeners;

    /**
     * Rot to watch
     */
    private Path pathToWatch;

    /**
     * Create a new watcher and register the existing folder
     * @param path
     * @throws IOException
     */
    public FileSystemWatcher (Path path) throws IOException {
        pathToWatch = path;
        keys = new HashMap<>();
        listeners = new HashMap<>();
        watchService = FileSystems.getDefault().newWatchService();

        // Register the already existing folder
        registerFolder(pathToWatch);

    }

    /**
     * Register (start watching) a new folder
     * @param path Path of the folder to watch
     * @throws IOException
     */
    private void registerFolder (Path path) throws IOException{

        /**
         * Walk trough the folder inside this folder
         */
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                // Register this folder
                WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

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
        for (;;) {
            try {
                WatchKey watchKey = watchService.take();

                Path dir = keys.get(watchKey);

                for(WatchEvent<?> event : watchKey.pollEvents()) {

                    final WatchEvent.Kind kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path name = ev.context();
                    Path changePath = dir.resolve(name);

                    final File file = changePath.toFile();
                    
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
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                }

                if(!watchKey.reset())
                    keys.remove(watchKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Interface of the event listener
     */
    public interface Listener {
        public void onEvent (File file);
    }
}
