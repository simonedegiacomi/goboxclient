package it.simonedegiacomi.sync.fs;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple File System Watcher
 *
 * Created on 24/12/2015.
 * @author Degiacomi Simone
 */
public class JNotifyFileSystemWatcher extends MyFileSystemWatcher {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(JNotifyFileSystemWatcher.class);

    /**
     * Set of listeners
     */
    private Set<FileSystemEventListener> listeners = new HashSet<>();

    /**
     * Watcher id of the JNotify library
     */
    private final int watcherId;

    /**
     * Create a new watcher and register the existing folder
     * @param path Path to watch
     * @throws IOException thrown watching this path
     */
    public JNotifyFileSystemWatcher(String path) throws IOException {
        watcherId = JNotify.addWatch(path, JNotify.FILE_ANY, true, new JNotifyListener() {

            @Override
            public void fileCreated(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (shouldIgnore(newFile)) {
                    return;
                }
                for (FileSystemEventListener listener : listeners) {
                    listener.onFileCreated(newFile);
                }
            }

            @Override
            public void fileDeleted(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (shouldIgnore(newFile)) {
                    return;
                }
                for (FileSystemEventListener listener : listeners) {
                    listener.onFileDeleted(newFile);
                }
            }

            @Override
            public void fileModified(int wd, String rootPath, String name) {
                File newFile = new File(rootPath + name);
                if (shouldIgnore(newFile)) {
                    return;
                }
                for (FileSystemEventListener listener : listeners) {
                    listener.onFileModified(newFile);
                }
            }

            @Override
            public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
                File oldFile = new File(rootPath + oldName);
                File newFile = new File(rootPath + newName);
                if (shouldIgnore(oldFile) && shouldIgnore(newFile)) {
                    return;
                }
                for (FileSystemEventListener listener : listeners) {
                    listener.onFileMoved(oldFile, newFile);
                }
            }
        });
    }

    public void addListener (FileSystemEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Stop watching for new file system events
     */
    public void shutdown () {
        try {
            JNotify.removeWatch(watcherId);
        } catch (JNotifyException ex) {
            log.warn(ex.toString(), ex);
        }
    }
}