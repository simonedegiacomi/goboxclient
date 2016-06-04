package it.simonedegiacomi.sync.fs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class MyFileSystemWatcher {

    public final static long CONTINUE_IGNORE_TIME = -1;

    public final static long FORESEE_FAKE_TIME = -2;

    /**
     * Map of files to ignore
     */
    protected final Map<String, Long> filesToIgnore = new HashMap<>();

    public static MyFileSystemWatcher getDefault(String pathToWatch) throws IOException {
        return new JavaFileSystemWatcher(pathToWatch);
        //return new JNotifyFileSystemWatcher(pathToWatch);
    }

    public abstract void shutdown () throws InterruptedException;

    public abstract void addListener (FileSystemEventListener newListener);

    /**
     * Check if a event relative to this file should be ignored or not
     * @param file File to check
     * @return Ignore or not
     */
    protected final boolean shouldIgnore (File file) {

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

                if (time == FORESEE_FAKE_TIME) {

                    // Ignore ONLY this time
                    filesToIgnore.remove(toIgnore);
                    return true;
                }

                // If the file was ignored before the file event, ignore it
                if (time < 0 || time >= file.lastModified()) {
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
     * Ignore all the events relative to this file. If this file is a folder, all the children
     * events will be ignored
     * @param file File to ignore
     */
    public void startIgnoring (File file) {
        filesToIgnore.put(file.getPath(), CONTINUE_IGNORE_TIME);
    }

    /**
     * Stop ignoring a file and receive new events. If this file is a folder, this is applied also
     * to his children.
     * NOTE that the file must be equivalent to the file that was passed as argument to the
     * {@link #startIgnoring(File) startIgnoring} method.
     * @param file File to stop ignoring
     */
    public void stopIgnoring (File file) {
        filesToIgnore.put(file.getPath(), System.currentTimeMillis() + 25);
    }

    /**
     * Ignore only the first event of the file. If you use this method, you can call stopIgnoring anyway.
     * In yuu need to ignore the incoming events of the file, see {@link #startIgnoring(File)}.
     * @param file File to which ignore ONLY the first event
     */
    public void foresee (File file) {
        filesToIgnore.put(file.getPath(), FORESEE_FAKE_TIME);
    }

    public interface FileSystemEventListener {

        void onFileCreated(File newFile);

        void onFileModified(File modifiedFile);

        void onFileDeleted(File deletedFile);

        void onFileMoved(File before, File movedFile);
    }
}