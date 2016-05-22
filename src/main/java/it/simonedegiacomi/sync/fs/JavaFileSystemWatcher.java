package it.simonedegiacomi.sync.fs;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CountDownLatch;


/**
 * Created on 12/05/16.
 *
 * @autor Degiacomi Simone
 */
public class JavaFileSystemWatcher extends MyFileSystemWatcher {

    /**
     * Logger of the class
     */
    private final static Logger logger = Logger.getLogger(MyFileSystemWatcher.class);

    private final WatchEvent.Kind[] watcherKinds = new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE};

    /**
     * Watch service
     */
    private final WatchService watchService;

    /**
     * Map with all the watcher keys
     */
    private final Map<WatchKey, Path> keys;

    /**
     * Set of listeners
     */
    private Set<FileSystemEventListener> listeners = new HashSet<>();

    private final CountDownLatch shutdown = new CountDownLatch(2);

    public JavaFileSystemWatcher(String path) throws IOException {

        // Get the file system watcher service
        watchService = FileSystems.getDefault().newWatchService();

        // Create a new map for the keys (key -> path)
        keys = new HashMap<>();

        watch(new File(path).toPath());

        startWatching();
    }

    /**
     * Start watching the specified folder
     *
     * @param path Folder to watch
     * @throws IOException
     */
    private void watch(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {

                // Get the watch key of the file
                WatchKey pathKey = path.register(watchService, watcherKinds, SensitivityWatchEventModifier.HIGH);

                // add the key to the map
                keys.put(pathKey, path);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Check if the file pointed to the specified path is moved
     *
     * @param path Path to check
     * @return Null if not moved, the old path if moved
     * @throws IOException bad things
     */
    private Path isMoved(Path path) throws IOException {

        // Create a new path key, equals to the old one (if exists)
        WatchKey pathKey = path.register(watchService, watcherKinds, SensitivityWatchEventModifier.HIGH);

        return keys.get(pathKey);
    }

    @Override
    public void shutdown() throws InterruptedException {
        shutdown.countDown();
        shutdown.await();
    }

    @Override
    public void addListener(FileSystemEventListener newListener) {
        listeners.add(newListener);
    }

    /**
     * Start a new thread and watch for changes ont he filesystem
     */
    private void startWatching() {
        Thread listenerThread = new Thread(() -> {
            while (shutdown.getCount() >= 2) {

                // find the key for the changed file
                WatchKey currentKey;
                try {
                    currentKey = watchService.take();
                } catch (InterruptedException ex) {
                    continue;
                }

                List<WatchEvent<?>> events = currentKey.pollEvents();

                for (WatchEvent<?> event : events) {

                    // Get the event
                    WatchEvent.Kind kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    // Find the path of the change file
                    WatchEvent<Path> eventFile = (WatchEvent<Path>) event;
                    Path name = eventFile.context();
                    Path filePath = keys.get(currentKey).resolve(name);


                    System.out.println(kind + " " + filePath + "(" + events.size());


                    if (shouldIgnore(filePath.toFile())) {
                        continue;
                    }


                    // File created
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        // If is  a folder, watch the folder
                        if (filePath.toFile().isDirectory()) {
                            try {

                                // But first check  if the folder was moved
                                Path oldPath = isMoved(filePath);

                                if (oldPath != null) {
                                    logger.info("file moved from " + oldPath + " to " + filePath);

                                    for (FileSystemEventListener listener : listeners) {
                                        listener.onFileMoved(oldPath.toFile(), filePath.toFile());
                                    }
                                }

                                // Watch the new folder
                                watch(filePath);

                                if (oldPath != null)
                                    continue;
                            } catch (IOException ex) {
                                logger.warn("cannot watch new file", ex);
                            }
                        }

                        if (!filePath.toFile().isDirectory() && events.size() == 2) {
                            WatchEvent<Path> oldEventFile = (WatchEvent<Path>) events.get(1);
                            Path oldName = oldEventFile.context();
                            Path oldFilePath = keys.get(currentKey).resolve(oldName);
                            for (FileSystemEventListener listener : listeners)
                                listener.onFileMoved(oldFilePath.toFile(), filePath.toFile());
                            continue;
                        }

                        // Nope, new file
                        logger.info("File created " + filePath);
                        for (FileSystemEventListener listener : listeners) {
                            listener.onFileCreated(filePath.toFile());
                        }
                        continue;
                    }

                    // File deleted
                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        logger.info("File deleted " + filePath);
                        for (FileSystemEventListener listener : listeners) {
                            listener.onFileDeleted(filePath.toFile());
                        }
                        continue;
                    }

                    logger.info("File modified " + filePath);
                    // File modified
                    for (FileSystemEventListener listener : listeners) {
                        listener.onFileModified(filePath.toFile());
                    }
                }

                if (!currentKey.reset()) {
                    logger.warn("invalid watch key");
                }
            }
        });
        listenerThread.setName("FileSystemListener");
        listenerThread.start();
    }
}