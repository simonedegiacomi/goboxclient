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
import it.simonedegiacomi.storage.StorageException;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBComponent;
import it.simonedegiacomi.storage.components.core.utils.DBCommonUtils;
import it.simonedegiacomi.storage.utils.MyFileUtils;
import it.simonedegiacomi.sync.fs.MyFileSystemWatcher;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class Move implements GBComponent {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(Move.class);

    private final Gson gson = MyGsonBuilder.create();

    /**
     * Database file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Database event table
     */
    private Dao<SyncEvent, Long> eventTable;

    private MyFileSystemWatcher fileSystemWatcher;

    private EventEmitter eventEmitter;

    private String PATH;

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

    @WSQuery(name = "move")
    public JsonElement onQuery(JsonElement data) {

        log.info("New move query");

        // Prepare the response
        JsonObject json = data.getAsJsonObject();
        JsonObject response = new JsonObject();

        if (!json.has("src") || !json.has("dst")) {
            response.addProperty("success", false);
            response.addProperty("error", "missing file");
            return response;
        }

        boolean copy = json.has("copy") && json.get("copy").getAsBoolean();

        // Instance the file from the json
        GBFile src = gson.fromJson(json.get("src"), GBFile.class);
        GBFile dst = gson.fromJson(json.get("dst"), GBFile.class);

        try {

            // Get the file with all the information
            GBFile dbSrc = DBCommonUtils.getFile(fileTable, src);

            // Find the destination father
            GBFile father = DBCommonUtils.getFile(fileTable, dst.getFather());

            if (dbSrc == null || father == null) {
                log.warn("File not found");
                response.addProperty("success", false);
                response.addProperty("error", "File not found");
                return response;
            }

            // Find path of the source and the father
            DBCommonUtils.findPath(fileTable, dbSrc);
            DBCommonUtils.findPath(fileTable, father);
            dbSrc.setPrefix(PATH);
            father.setPrefix(PATH);

            // Generate the new child
            dst = father.generateChild(dst.getName(), src.isDirectory());

            // TODO: clean this
            while (dst.toFile().exists()) {
                log.info("Destination file already exists");
                String name = dst.getName();
                dst.setName(name.substring(0, name.lastIndexOf(".")) + " - Copy" + name.substring(name.lastIndexOf("."), name.length()));
            }

            // Tell the fs watcher to ignore this event (the copy)
            fileSystemWatcher.startIgnoring(dbSrc.toFile());
            fileSystemWatcher.startIgnoring(dst.toFile());

            SyncEvent event;
            if (copy) {

                // Copy the file to the new destination
                MyFileUtils.copyR(dbSrc.toFile(), dst.toFile());

                // Update the database
                MyFileUtils.loadFileAttributes(dst);
                fileTable.create(dst);

                event = new SyncEvent(SyncEvent.EventKind.FILE_COPIED, dst);
                event.setBefore(dbSrc);
            } else {

                // Just move the file
                Files.move(dbSrc.toFile().toPath(), dst.toFile().toPath());

                // Update the database
                dbSrc.setFatherID(dst.getFatherID());
                dbSrc.setName(dst.getName());
                dbSrc.setLastUpdateDate(System.currentTimeMillis());
                fileTable.update(dbSrc);

                event = new SyncEvent(SyncEvent.EventKind.FILE_MOVED, dst);
                event.setBefore(dbSrc);
            }

            // Register and emit the event
            eventTable.create(event);
            eventEmitter.emitEvent(event);

            // Stop ignoring this file
            fileSystemWatcher.stopIgnoring(dbSrc.toFile());
            fileSystemWatcher.stopIgnoring(dst.toFile());

            // Complete the response
            response.addProperty("success", true);
        } catch (IOException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", "IO error");
            return response;
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            response.addProperty("success", false);
            response.addProperty("error", ex.toString());
            return response;
        }

        return response;
    }
}