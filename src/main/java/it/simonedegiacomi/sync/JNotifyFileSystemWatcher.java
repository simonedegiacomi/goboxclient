package it.simonedegiacomi.sync;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Simple File System Watcher
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
 */
public class JNotifyFileSystemWatcher extends MyFileSystemWatcher {

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
    public JNotifyFileSystemWatcher(String path, FileSystemEventListener listener) throws IOException {

        watcherId = JNotify.addWatch(path, JNotify.FILE_ANY, true, new JNotifyListener() {

            @Override
            public void fileCreated(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (!shouldIgnore(newFile)) {
                    listener.onFileCreated(newFile);
                }
            }

            @Override
            public void fileDeleted(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (!shouldIgnore(newFile)) {
                    listener.onFileDeleted(newFile);
                }
            }

            @Override
            public void fileModified(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (!shouldIgnore(newFile)) {
                    listener.onFileModified(newFile);
                }
            }

            @Override
            public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
                File oldFile = new File(rootPath + oldName);
                File newFile = new File(rootPath + newName);
                if (!shouldIgnore(oldFile) && !shouldIgnore(newFile)) {
                    listener.onFileMoved(oldFile, newFile);
                }
            }
        });
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
    public void shutdown () throws JNotifyException {
        JNotify.removeWatch(watcherId);
    }
}