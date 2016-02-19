package it.simonedegiacomi.sync;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple File System Watcher
 *
 * Created by Degiacomi Simone onEvent 24/12/2015.
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

    private final Set<String> filesToIgnore = new HashSet<>();

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
        for (;;) {
            try {
                WatchKey watchKey = watchService.take();

                Path dir = keys.get(watchKey);

                for(WatchEvent<?> event : watchKey.pollEvents()) {

                    final WatchEvent.Kind kind = event.kind();

                    System.out.printf("KIND: %s ", kind.toString());

                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path name = ev.context();
                    System.out.printf(" File: %s", name);
                    if(name.getFileName().toFile().getName().contains(".DS_Store"))
                        continue;
                    Path changePath = dir.resolve(name);

                    final File file = changePath.toFile();

                    if(filesToIgnore.remove(file.toString()))
                        continue;
                    
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("New thread working with " + file.toString());
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

    public void ignore(File file) {
        filesToIgnore.add(file.toString());
    }

    /**
     * Interface of the event listener
     */
    public interface Listener {
        public void onEvent (File file);
    }

}
