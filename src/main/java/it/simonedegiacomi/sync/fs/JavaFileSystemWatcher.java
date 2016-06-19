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

                logger.info("Start watching " + path);

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
                logger.info("Acquired new events: " + events.size());

                // Create a new list
                LinkedList<WatchEvent<?>> orderedEvents = new LinkedList<>(events);

                // Sort it
                Collections.sort(orderedEvents, (a, b) -> a.kind().equals(StandardWatchEventKinds.ENTRY_DELETE) ? -1 : 1);

                Path deletedFile = null;

                for (WatchEvent<?> event : orderedEvents) {

                    // Get the event
                    WatchEvent.Kind kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        logger.warn("Event overflow! possible lost of events!");
                        continue;
                    }

                    // Find the path of the changed file
                    WatchEvent<Path> eventFile = (WatchEvent<Path>) event;
                    Path name = eventFile.context();
                    Path filePath = keys.get(currentKey).resolve(name);
                    logger.info("Event " + event.kind() + " " + filePath + " at " + filePath.toFile().lastModified());

                    // Start watching if it is a folder
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && filePath.toFile().isDirectory()) {
                        try {
                            watch(filePath);
                        } catch (IOException ex) {
                            logger.warn("Can't start watching the new folder! possible lost of events!");
                        }
                    }

                    // File deleted
                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        logger.info("Deleted event " + filePath);
                        deletedFile = filePath;
                        continue;
                    }

                    // Check if i should ignore the event
                    if (shouldIgnore(filePath.toFile())) {
                        logger.info("Event " + kind + " on " + filePath + " ignored");
                        continue;
                    }

                    // File created
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        // Not a folder? check if the file is moved
                        if (events.size() == 2 && deletedFile != null) {
                            for (FileSystemEventListener listener : listeners)
                                listener.onFileMoved(deletedFile.toFile(), filePath.toFile());
                            deletedFile = null;
                            break;
                        }

                        // New file
                        logger.info("File created " + filePath);

                        // notify the creation of the file/folder
                        for (FileSystemEventListener listener : listeners) {
                            listener.onFileCreated(filePath.toFile());
                        }

                        continue;
                    }

                    // File modified
                    logger.info("File modified " + filePath);
                    for (FileSystemEventListener listener : listeners) {
                        listener.onFileModified(filePath.toFile());
                    }
                }

                if (deletedFile != null && !shouldIgnore(deletedFile.toFile())) {
                    for (FileSystemEventListener listener : listeners)
                        listener.onFileDeleted(deletedFile.toFile());
                }

                // Is the key still valid?
                if (!currentKey.reset()) {
                    logger.warn("Invalid watch key");
                }
            }
        });
        listenerThread.setName("FileSystemListener");
        listenerThread.start();
    }
}