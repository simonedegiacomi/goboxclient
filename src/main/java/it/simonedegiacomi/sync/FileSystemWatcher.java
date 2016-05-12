package it.simonedegiacomi.sync;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple File System Watcher
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
 */
public class FileSystemWatcher {

    /**
     * Map of files to ignore
     */
    private final Map<String, Long> filesToIgnore = new HashMap<>();

    private final int watcherId;

    /**
     * Create a new watcher and register the existing folder
     * @param path Path to watch
     * @throws IOException thrown watching this path
     */
    public FileSystemWatcher (String path, FileSystemEventListener listener) throws IOException {

        watcherId = JNotify.addWatch(path, JNotify.FILE_ANY, true, new JNotifyListener() {

            @Override
            public void fileCreated(int wd, String rootPath, String name) {
                if (!shouldIgnore()) {
                    listener.onFileCreated(new File(name));
                }
            }

            @Override
            public void fileDeleted(int wd, String rootPath, String name) {
                if (!shouldIgnore()) {
                    listener.onFileDeleted(new File(name));
                }
            }

            @Override
            public void fileModified(int wd, String rootPath, String name) {
                if (!shouldIgnore()) {
                    listener.onFileModified(new File(name));
                }
            }

            @Override
            public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
                if (!shouldIgnore()) {
                    listener.onFileMoved(new File(oldName), new File(newName));
                }
            }
        });
    }

    public interface FileSystemEventListener {

        void onFileCreated (File newFile);

        void onFileModified (File modifiedFile);

        void onFileDeleted (File deletedFile);

        void onFileMoved (File before, File movedFile);
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
     * Stop watching for new file system events
     */
    public void shutdown () {
        if (watcherId <= 0)
            throw new IllegalStateException("watcher already off");
        JNotify.removeWatch(watcherId);
        watcherId = -1;
    }
}