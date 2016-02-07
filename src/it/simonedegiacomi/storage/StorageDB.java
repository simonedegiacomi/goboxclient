package it.simonedegiacomi.storage;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.JdbcDatabaseConnection;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class wrap the database in a object that
 * exposes all the method used in the GoBox Storage.
 *
 * Created by Degiacomi Simone onEvent 23/12/2015
 */
public class StorageDB {

    private static final Logger log = Logger.getLogger(StorageDB.class.getName());

    private ConnectionSource connectionSource;

    /**
     * Connection to the database
     */
    private Connection db;

    private Dao<GBFile, Long> fileTable;

    private Dao<SyncEvent, Long> eventTable;

    /**
     * Create a new database and open the connection
     * @param path Path to the database
     * @throws Exception Exception in case there are
     * some problem with the connection or with the
     * initialization. If any exception are trowed,
     * the object shouldn't be used
     */
    public StorageDB(String path) throws Exception {

        // Connect to the database
        connectionSource = new JdbcConnectionSource("jdbc:h2:" + path);

        // Get a connection for the raw queries
        db = ((JdbcDatabaseConnection) connectionSource.getReadWriteConnection()).getInternalConnection();
        db.setAutoCommit(true);

        // Create the DAO object. I'll use this object to simplify the insertion of the GBFiles
        fileTable = DaoManager.createDao(connectionSource, GBFile.class);
        fileTable.setAutoCommit(connectionSource.getReadWriteConnection(), true);
        eventTable = DaoManager.createDao(connectionSource, SyncEvent.class);
        eventTable.setAutoCommit(connectionSource.getReadWriteConnection(), true);

        log.info("Connected to local H2 database");
        try {
            // Initialize the tables
            initDatabase();
            connectionSource.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.log(Level.SEVERE, ex.toString(), ex);
            // If there is any exception with the initialization
            // close the connection
            connectionSource.close();
            db.close();
        }
    }

    /**
     * Close the connection
     * @throws SQLException
     */
    public void close () throws SQLException {
        db.close();
        connectionSource.close();
        log.info("Database disconnected");
    }

    /**
     * Init the database tables
     * @throws Exception Exception if the creation of
     * the tables fails.
     */
    private void initDatabase () throws Exception {

        // Create the file table
        TableUtils.createTableIfNotExists(connectionSource, GBFile.class);

        // Create the event table
        TableUtils.createTableIfNotExists(connectionSource, SyncEvent.class);

        // Check if the root file is already in the database
        if(fileTable.queryForId(GBFile.ROOT_ID) == null) {
            GBFile root = new GBFile("root", true);
            root.setID(GBFile.ROOT_ID);
            root.setFatherID(GBFile.UNKNOWN_FATHER); // I'm not sure about it...
            fileTable.create(root);
        }

        log.fine("Database tables initialized.");
    }

    /**
     * Find the ID of the file and set it in the GBFile. This method works only when
     * the path is set. If the file is not in the database, only the father ID will
     * be found, same for his father etc..
     *
     * So, if you call this method when the file 'music/song.mp3' in not inserted yet
     * into the database, as result you'll have the file music with his ID and father ID
     * (the root in this case) and the father ID in 'song.mp3'
     *
     * TL DR use this method to find the father
     */
    public void findID (GBFile file) {
        // Just need to find the father, so get the path in a list
        List<GBFile> path = file.getPathAsList();

        try {
            // Prepare the query that is the same each iteration
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM file WHERE father_ID = ? AND name = ?");

            // Start with the only file where i'm sure o know his father, the root
            long fatherOfSomeone = GBFile.ROOT_ID;

            // Find the father of every ancestor node
            for(GBFile ancestor : path) {
                ancestor.setFatherID(fatherOfSomeone);

                // Query the database
                stmt.setLong(1, fatherOfSomeone);
                stmt.setString(2, ancestor.getName());
                ResultSet res = stmt.executeQuery();

                // If there is no result
                if(!res.isBeforeFirst())
                    return;
                fatherOfSomeone = res.getLong("ID");
                res.close();
                ancestor.setID(fatherOfSomeone);
            }

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    /**
     * Find the path of the file and set it into the file
     * @param file File that doesn't know his path
     */
    public void findPath (GBFile file) {
        file.setPathByList(findPathAsList(file));
    }

    /**
     * Find thge path of the file. This method works only if the father ID of the
     * GBFile is set
     * @param file File that doesn't know his path
     * @return Array of GBFiles that are the nodes in hierarchy order
     */
    public List<GBFile> findPathAsList(GBFile file) {

        // Create a new temporary list
        List<GBFile> path = new LinkedList<>();


        long fatherId = file.getFatherID();
        while (fatherId != GBFile.ROOT_ID) {
            GBFile node = this.getFileById(fatherId);
            fatherId = node.getFatherID();
            path.add(0, node);
        }

        // add in the last position the file
        path.add(file);

        return path;
    }

    /**
     * Insert a new file in the database
     * @param newFile New file to insert. The object will be
     *                filled with the new information obtained
     *                inserting the data (the ID)
     * @throws Exception Exception throwed during the insertion
     */
    public SyncEvent insertFile (GBFile newFile) throws Exception {
        // If the file doesn't know his father, let's find his
        if (newFile.getFatherID() == GBFile.UNKNOWN_FATHER)
            findID(newFile);

        fileTable.create(newFile);

        log.info("New file inserted onEvent the database");

        // Create the SyncEvent to return
        SyncEvent event = new SyncEvent(SyncEvent.EventKind.NEW_FILE, newFile);

        // And add it to the right db table
        registerEvent(event);

        return event;
    }

    /**
     * Add a new row in the event table
     * @param event Event to add
     */
    private void registerEvent (SyncEvent event) {
        try {
            eventTable.create(event);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    /**
     * Update the information of a file
     * @param updatedFile File to update with the new information
     * @return Generated sync event
     */
    public SyncEvent updateFile (GBFile updatedFile) {
        try {
            // If the file doesn't know his id, let's find it
            if(updatedFile.getID() == GBFile.UNKNOWN_ID)
                findID(updatedFile);

            // Update the file table
            fileTable.update(updatedFile);

            // Create sync event
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.EDIT_FILE, updatedFile);
            registerEvent(event);
            return  event;
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        return null;
    }

    /**
     * Remove a file from the database
     * @param fileToRemove File to remove
     * @return The generated sync event
     */
    public SyncEvent removeFile (GBFile fileToRemove) {
        try {
            // If the file doesn't know his id, let's find it
            if(fileToRemove.getID() == GBFile.UNKNOWN_ID)
                findID(fileToRemove);

            // Remove from the database
            fileTable.deleteById(fileToRemove.getID());

            // Create the sync event
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.REMOVE_FILE, fileToRemove);
            registerEvent(event);

            //TODO: Remove all the children

            return event;
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        return null;
    }

    /**
     * Query the database to find the children of a directory. this method works only if
     * the ID of the file is set
     * @param fatherID ID of the father directory
     * @return Array of children
     * @throws StorageException
     */
    public List<GBFile> getChildrenByFather (GBFile father) throws StorageException {
        long fatherID = father.getID();
        List<GBFile> children = new ArrayList<>();
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE father_ID = ? ORDER BY name");
            stmt.setLong(1, fatherID);
            ResultSet sqlResults = stmt.executeQuery();

            while(sqlResults.next()) {
                GBFile temp = new GBFile(sqlResults.getInt("ID"), fatherID,
                        sqlResults.getString("name"), sqlResults.getBoolean("is_directory"));
                temp.setSize(sqlResults.getLong("size"));
                temp.setCreationDate(sqlResults.getLong("creation"));
                temp.setLastUpdateDate(sqlResults.getLong("last_update"));
                children.add(temp);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
        }
        return children;
    }

    /**
     * Query the database and return the requested file
     * @param id File Id to search in the database
     * @param children If true, insert in the file also his children
     * @return File retrieved from the database
     */
    public GBFile getFileById (long id, boolean path, boolean children) {
        try {
            // Get the GBFile
            GBFile file = fileTable.queryForId(id);
            if(children && file.isDirectory())
                file.setChildren(this.getChildrenByFather(file));
            if(path)
                file.setPathByList(this.findPathAsList(file));
            return file;
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            return null;
        }
    }

    /**
     * Query the database and return the GBFile. Either the children and
     * path fields of this object will be null
     * @param id ID of the file
     * @return The wrapped GBFile
     */
    public GBFile getFileById (long id) {
        return this.getFileById(id, false, false);
    }

    public List<SyncEvent> getUniqueEventsFromID (long ID) {

        // List that will contains all the events
        List<SyncEvent> events = new LinkedList<>();

        try {

            String stringID = String.valueOf(ID);

            GenericRawResults<SyncEvent> eventsRaw = eventTable.queryRaw("SELECT * FROM event," +
                    "(SELECT file_ID, MAX(date) as lastDate FROM event WHERE ID > ? GROUP BY file_ID) AS latestEvent" +
                    "WHERE ID > ? AND" +
                    "latestEvent.file_ID = event.file_ID AND event.date = latestEvent.lastDate", eventTable.getRawRowMapper(), stringID, stringID);

            for(SyncEvent event : eventsRaw)
                events.add(event);

            eventsRaw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return events;
    }
}