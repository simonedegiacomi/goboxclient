package it.simonedegiacomi.sync;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.ClientException;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

/**
 * The Employee object do works. He need the sync and the file system watcher to
 * ignore files that he is working with. The Employee can ask other employee (or himself
 * in the future) to do a works generated from one work (for example download a folder).
 * This class is thread-safe, so you can use the same employee to do concurrency works
 */
public class Employee {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(Employee.class);

    /**
     * Client to use to transfer files
     */
    private final GBClient client;

    /**
     * Sync object used to ignore generated events
     */
    private final Sync sync;

    /**
     * File system watcher used to ignore working files.
     */
    private final MyFileSystemWatcher watcher;

    /**
     * Manager used to ask other employees to do other stuff to help this employee
     */
    private final WorkManager manager;

    private final String PATH = Config.getInstance().getProperty("path");


    public Employee(GBClient client, Sync sync, WorkManager workManager) {
        this.client = client;
        this.sync = sync;
        this.watcher = sync.getFileSystemWatcher();
        this.manager = workManager;
    }

    public boolean submit (Work workToDo) {
        workToDo.setState(Work.WorkState.RUNNING);

        // Start to ignore the file in the local file system
        watcher.startIgnoring(workToDo.getFile().toFile());

        try {

            switch (workToDo.getKind()) {
                case DOWNLOAD:
                    download(workToDo.getFile());
                    break;

                case UPLOAD:
                    upload(workToDo.getFile());
                    break;

                case MOVE_IN_CLIENT:

                    Files.move(workToDo.getBefore().toFile().toPath(), workToDo.getFile().toFile().toPath());
                    break;

                case MOVE_IN_STORAGE:

                    client.move(workToDo.getBefore(), workToDo.getFile(), false);
                    break;

                case REMOVE_IN_CLIENT:

                    MyFileUtils.delete(workToDo.getFile());
                    break;

                case REMOVE_IN_STORAGE:

                    client.removeFile(workToDo.getFile());
                    break;
            }

            workToDo.setState(Work.WorkState.END);
            return true;
        } catch (Exception ex) {
            workToDo.setState(Work.WorkState.FAILED);
            log.warn(ex.toString(), ex);
            return false;
        }
    }


    /**
     * Download the file if a real file, schedule multiple download if a folder
     * @param file File to download
     * @throws IOException
     * @throws ClientException
     */
    private void download (GBFile file) throws IOException, ClientException {

        // Get the info of the file
        GBFile detailedFile = client.getInfo(file);
        detailedFile.setPrefix(file.getPrefix());

        // If the file is a directory
        if (detailedFile.isDirectory()) {

            // Download each file
            for (GBFile child : detailedFile.getChildren()) {
                manager.addWork(new Work(child, Work.WorkKind.DOWNLOAD));
            }
        }

        // Otherwise just download the file
        client.getFile(file);
    }

    /**
     * Download the file if a file, schedule multiple upload if a folder
     * @param file File to upload
     * @throws ClientException
     * @throws IOException
     */
    private void upload (GBFile file) throws ClientException, IOException {

        // If the file is a folder
        if (file.isDirectory()) {

            // Create the folder in the storage
            client.createDirectory(file);

            // And create enw works, to upload each file
            for (GBFile child : file.getChildren()) {
                manager.addWork(new Work(child, Work.WorkKind.UPLOAD));
            }

            return;
        }

        // Upload the file
        client.uploadFile(file);
    }
}