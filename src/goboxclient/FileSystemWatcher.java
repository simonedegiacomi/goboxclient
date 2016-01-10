package goboxclient;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class FileSystemWatcher extends Thread {

    public static final String FILE_CREATED = StandardWatchEventKinds.ENTRY_CREATE.name();
    public static final String FILE_DELETED = StandardWatchEventKinds.ENTRY_DELETE.name();
    public static final String FILE_CHANGED = StandardWatchEventKinds.ENTRY_MODIFY.name();

    private WatchService watchService;

    private HashMap<WatchKey, Path> keys;

    private HashMap<String, Listener> listeners;

    private Path pathToWatch;

    public FileSystemWatcher (Path path) throws IOException {
        pathToWatch = path;
        keys = new HashMap<>();
        listeners = new HashMap<>();
        watchService = FileSystems.getDefault().newWatchService();

        // Register the already existing folder


        registerFolder(pathToWatch);

    }

    private void registerFolder (Path path) throws IOException{
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                // Register this folder
                WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);


                keys.put(key, dir);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void assignListener (String eventName, Listener listener) {
        listeners.put(eventName, listener);
    }

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

    public interface Listener {
        public void onEvent (File file);
    }
}
