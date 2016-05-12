package it.simonedegiacomi.sync;

import java.io.File;

public abstract class MyFileSystemWatcher {

    public abstract void startIgnoring (File file);

    public abstract void stopIgnoring (File file);

    public abstract void shutdown () throws Exception;

    public interface FileSystemEventListener {

        void onFileCreated(File newFile);

        void onFileModified(File modifiedFile);

        void onFileDeleted(File deletedFile);

        void onFileMoved(File before, File movedFile);
    }
}