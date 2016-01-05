package goboxclient;

import configuration.Config;
import goboxapi.GBFile;
import goboxapi.client.StandardClient;
import storage.StorageDB;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;


public class Sync {

    private StandardClient client;
    private final Config config = Config.getInstance();
    private FileSystemWatcher watcher;

    public Sync (StandardClient client) {
        this.client = client;
        // Create the new watcher for the fileSystem
        try {
            Path pathToWatch = new File("files/").toPath();
            watcher = new FileSystemWatcher(pathToWatch);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Assign the event of the file watcher
        assignFileWatcherEvents();
        watcher.start();
    }

    private void assignFileWatcherEvents () {
        // At the beginning i download the file list, and make a control.
        // I donwload the file from the sevrer, and new files found are trasmetted to sevrer.
        // If  a file wa sdeleted on the client with the program nmot running, the file will
        // redonloaded

        watcher.assignListener(FileSystemWatcher.FILE_CREATED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File newFile) {
                System.out.println("Nuovo file: " + newFile);
                // Wrap the java File into a GoBoxFile
                GBFile wrappedFile = new GBFile(newFile);
                System.out.println(wrappedFile);
                // Tell the server the hash
                // Upload the file to the server
                client.uploadFile(wrappedFile);
            }
        });
        watcher.assignListener(FileSystemWatcher.FILE_CHANGED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File editedFile) {
                System.out.println(editedFile);
                // Wrap the java File into a GoBoxFile
                GBFile wrappedFile = new GBFile(editedFile);
                //client.updateFile(wrappedFile);
            }
        });

        watcher.assignListener(FileSystemWatcher.FILE_DELETED, new FileSystemWatcher.Listener() {
            @Override
            public void onEvent(File deletedFile) {
                System.out.println(deletedFile);
                GBFile wrappedFile = new GBFile(deletedFile);
                client.removeFile(wrappedFile);
            }
        });
    }

    public void assignEventsFromServer () {

    }
}
