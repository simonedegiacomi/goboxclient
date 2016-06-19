package it.simonedegiacomi.storage.components.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.storage.EventEmitter;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class CreateFolder implements GBModule {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(CreateFolder.class);

    private final Gson gson = MyGsonBuilder.create();

    /**
     * Path prefix of the environment
     */
    private String PATH;

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database event table
     */
    private Dao<SyncEvent, Long> eventTable;

    private EventEmitter eventEmitter;

    private MyFileSystemWatcher fileSystemWatcher;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        fileSystemWatcher = env.getFileSystemWatcher();
        eventEmitter = env.getEmitter();
        PATH = env.getGlobalConfig().getProperty("path", "files/");
        try {
            fileTable = DaoManager.createDao(env.getDbConnection(), GBFile.class);
            eventTable = DaoManager.createDao(env.getDbConnection(), SyncEvent.class);
        } catch (SQLException ex) {
            log.warn("Unable to create dao", ex);
            throw new AttachFailException("Unable to create dao");
        }
    }

    @Override
    public void onDetach() {

    }

    @WSQuery(name = "createFolder")
    public JsonElement onQuery (JsonElement data) {
        log.info("CreateFolder query");
        JsonObject json = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        // Wrap and prepare the new directory
        GBFile newFolder = gson.fromJson(json, GBFile.class);
        newFolder.setLastUpdateDate(System.currentTimeMillis());
        newFolder.setPrefix(PATH);

        try {

            // Check if a folder with the same name already exists
            if (DBCommonUtils.exists(fileTable, newFolder)) {
                log.warn("Folder already exists");
                response.addProperty("success", false);
                response.addProperty("created", false);
                response.addProperty("error", "Folder already exists");
                return response;
            }

            // Find the father
            GBFile father = DBCommonUtils.getFile(fileTable, newFolder.getFather());

            if (father == null) {
                log.warn("Folder father not found");
                response.addProperty("success", false);
                response.addProperty("created", false);
                response.addProperty("error", "Father not found");
                return response;
            }

            // Find the path of the father
            DBCommonUtils.findPath(fileTable, father);
            father.setPrefix(PATH);

            // Create the child
            newFolder = father.generateChild(newFolder.getName(), true);

            // Tell the fs watcher to ignore this event
            fileSystemWatcher.startIgnoring(newFolder.toFile());

            // Create the real file in the FS
            Files.createDirectory(newFolder.toFile().toPath());

            // fill the response
            response.addProperty("newFolderId", newFolder.getID());
            response.addProperty("created", true);
            response.addProperty("success", true);

            // Insert in the database
            fileTable.create(newFolder);

            // Stop ignoring the file
            fileSystemWatcher.stopIgnoring(newFolder.toFile());

            // Register and send the event
            SyncEvent creation = new SyncEvent(SyncEvent.EventKind.FILE_CREATED, newFolder);
            eventTable.create(creation);
            eventEmitter.emitEvent(creation);

            log.info("Folder created");
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("created", false);
            response.addProperty("error", ex.toString());
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("created", false);
            response.addProperty("error", ex.toString());
        }

        // Finally return the response
        return response;
    }
}
