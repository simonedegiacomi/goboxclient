package it.simonedegiacomi.storage;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.JdbcDatabaseConnection;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import org.apache.log4j.Logger;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class wrap the database in a object that
 * exposes all the method used in the GoBox Storage.
 *
 * Created on 23/12/2015
 * @author Degiacomi Simone
 */
public class StorageDB {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(StorageDB.class.getName());

    /**
     * Connection to the database
     */
    private ConnectionSource connectionSource;

    /**
     * JDBC Connection to the database
     */
    private Connection db;

    /**
     * Dao file table
     */
    private Dao<GBFile, Long> fileTable;

    /**
     * Dao event table
     */
    private Dao<SyncEvent, Long> eventTable;

    /**
     * Dao sharing table
     */
    private Dao<Sharing, Long> sharingTable;

    /**
     * Create a new database and open the connection
     * @param path Path to the database
     * @throws Exception Exception in case there are some problem with the connection or with the
     * initialization. If any exception are trowed, the object shouldn't be used
     */
    public StorageDB(String path) throws StorageException {

        if (path == null || path.length() <= 0)
            throw new InvalidParameterException("Invalid path string");

        try {

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

            sharingTable = DaoManager.createDao(connectionSource, Sharing.class);
            sharingTable.setAutoCommit(connectionSource.getReadWriteConnection(), true);

            log.info("Connected to local H2 database");

            // Initialize the tables
            initDatabase();
        } catch (SQLException ex) {

            throw new StorageException("Can't connect to database");
        }
    }

    /**
     * Close the connection
     * @throws SQLException
     */
    public void close () throws SQLException {

        if (db == null)
            throw new IllegalStateException("The database connection is not open");

        db.close();
        connectionSource.close();
        log.info("Database disconnected");
    }

    /**
     * Init the database tables
     * @throws Exception Exception if the creation of the tables fails.
     */
    private void initDatabase () throws SQLException {

        // Create the file table
        TableUtils.createTableIfNotExists(connectionSource, GBFile.class);

        // Create the event table
        TableUtils.createTableIfNotExists(connectionSource, SyncEvent.class);

        // Create the sharing table
        TableUtils.createTableIfNotExists(connectionSource, Sharing.class);

        // Check if the root file is already in the database
        if(fileTable.queryForId(GBFile.ROOT_ID) == null) {

            GBFile root = new GBFile("root", true);
            root.setID(GBFile.ROOT_ID);
            root.setCreationDate(System.currentTimeMillis());
            root.setLastUpdateDate(System.currentTimeMillis());
            fileTable.create(root);
            System.out.println(root);
        }

        log.info("Database tables initialized.");
    }

    /**
     * Find the ID of the file and set it in the GBFile. This method works only when the path is set.
     * If the file is not in the database, only the father ID will be found, same for his father etc..
     * So, if you call this method when the file 'music/song.mp3' in not inserted yet into the database, as result
     * you'll have the file music with his ID and father ID (the root in this case) and the father ID in 'song.mp3'
     *
     * TL;DR use this method to find the father
     * @param file File to which find the id using the path
     */
    public void findIDByPath (GBFile file) throws StorageException {

        if (file == null)
            throw new InvalidParameterException("File can't be null");

        // Get the path list
        List<GBFile> path = file.getPathAsList();

        if (path == null)
            throw new InvalidParameterException("The file has a null path");

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
                if(!res.next())
                    return;

                fatherOfSomeone = res.getLong("ID");
                res.close();
                ancestor.setID(fatherOfSomeone);
            }

        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot find ID");
        }
    }

    /**
     * Find the path of the file. This method works only if the father ID of the GBFile is set.
     * However, if you call this method and the path field in the GBFile is set, the method 'findIDByPath'
     * will be called.
     * @param file File that doesn't know his path
     */
    public void findPath(GBFile file) throws StorageException {

        // Check if the file is null
        if (file == null)
            throw new InvalidParameterException("File can't be null");

        // Get the father id of the file
        long fatherId = file.getFatherID();

        // Check if it's known
        if(fatherId == GBFile.UNKNOWN_ID)
            throw new InvalidParameterException("This file doesn't know his father id");

        // Create a new temporary list
        List<GBFile> path = new LinkedList<>();

        // Until the father is the root
        while (fatherId != GBFile.ROOT_ID) {

            // find the father of the father
            GBFile node = this.getFileById(fatherId);
            fatherId = node.getFatherID();

            // Add this father tot he list
            path.add(0, node);
         }

        // Finally add the root
        path.add(0, GBFile.ROOT_FILE);

        // Add also the file as last element
        path.add(file);

        // And set the path to the file
        file.setPathByList(path);
    }

    /**
     * Insert a new file in the database
     * @param newFile New file to insert. The object will be filled with the new information obtained
     *                inserting the data (the ID)
     * @throws Exception Exception thrown during the insertion
     */
    public SyncEvent insertFile (GBFile newFile) throws StorageException {

        // Assert that the file is not null
        if (newFile == null)
            throw new InvalidParameterException("File insert a null file");

        // If the file doesn't know his father, let's find his
        if (newFile.getFatherID() == GBFile.UNKNOWN_ID)
            findIDByPath(newFile);

        try {

            // Insert into the database
            fileTable.create(newFile);
        } catch (SQLException ex) {
            throw new StorageException("Cannot insert file into the database");
        }

        log.info("New file inserted on the database");

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
    private void registerEvent (SyncEvent event) throws StorageException {

        // Assert that the event is not null
        if (event == null)
            throw new InvalidParameterException("Can't add null event");

        try {
            eventTable.create(event);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot insert event into database");
        }
    }

    /**
     * Update the information of a file
     * @param updatedFile File to update with the new information
     * @return Generated sync event
     */
    public SyncEvent updateFile (GBFile updatedFile) throws StorageException {

        // Check if the file is null
        if (updatedFile == null)
            throw new InvalidParameterException("File can't be null");

        try {
            // If the file doesn't know his id, let's find it
            if(updatedFile.getID() == GBFile.UNKNOWN_ID)
                findIDByPath(updatedFile);

            // Update the file table
            fileTable.update(updatedFile);

            // Create sync event
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.EDIT_FILE, updatedFile);
            registerEvent(event);
            return  event;
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot update file");
        }
    }

    /**
     * Remove a file from the database
     * @param fileToRemove File to remove
     * @return The generated sync event
     */
    public SyncEvent removeFile (GBFile fileToRemove) throws StorageException {

        // Check if the file is null
        if (fileToRemove == null)
            throw new InvalidParameterException("File can't be null");

        try {
            // If the file doesn't know his id, let's find it
            if(fileToRemove.getID() == GBFile.UNKNOWN_ID)
                findIDByPath(fileToRemove);

            // Remove from the database
            fileTable.deleteById(fileToRemove.getID());

            // Also his children
            if(fileToRemove.isDirectory())
                for(GBFile child : fileToRemove.getChildren())
                        removeFile(child);

            // Create the sync event
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.REMOVE_FILE, fileToRemove);
            registerEvent(event);

            return event;
        } catch (SQLException ex) {

            // TODO: Because i'am not sure if the error was with the first or the last query is better to rollback
            throw new StorageException("Cannot remove file");
        }
    }

    /**
     * Query the database to find the children of a directory. this method works only if
     * the ID of the file is set
     * @param fatherID ID of the father directory
     * @throws StorageException
     */
    public void findChildrenByFather(GBFile father) throws StorageException {
        try {
            QueryBuilder<GBFile, Long> stmt =  fileTable.queryBuilder();
            stmt.where().eq("father_ID", father.getID());
            stmt.orderBy("name", true);
            father.setChildren(stmt.query());
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot find children");
        }
    }

    /**
     * Fill the file with the information of the database, such as children and path
     * @param file Fill to fill with his information
     * @return The same file passed as argument
     */
    public GBFile fillFile (GBFile file, boolean path, boolean children) throws StorageException {

        // assert that the file is not null
        if (file == null)
            throw new InvalidParameterException("This method doesn't accept null file.");

        // Assert that the file know his id
        if (file.getID() == GBFile.UNKNOWN_ID) {

            if (file.getPathAsList() == null)
                throw new InvalidParameterException("The file hasn't enough information");

            // Find the id using the path
            findIDByPath(file);
        }

        // Assert that know his path
        if (path && file.getPathAsList() == null) {

            // Find it
            findPath(file);
        }

        // Assert that know his children
        if (children && file.getChildren() == null) {

            // Find them
            findChildrenByFather(file);
        }

        return file;
    }

    /**
     * Query the database and return the requested file.
     * This methodis just an alias for {@link #fillFile(GBFile, boolean, boolean)}
     * @param id File Id to search in the database
     * @param children If true, insert in the file also his children
     * @return File retrieved from the database
     */
    public GBFile getFileById (long id, boolean path, boolean children) throws StorageException {

        // Assert that the id is valid
        if (id <= 0)
            throw new InvalidParameterException("ID cannot be less than zero");

        return fillFile(new GBFile(id), path, children);
    }

    /**
     * Query the database and return the GBFile. Either the children and path fields of this object will be null.
     * This method is just an alias for {@link #getFileById(long, boolean, boolean)}
     * @param id ID of the file
     * @return The wrapped GBFile
     */
    public GBFile getFileById (long id) throws StorageException {
        return getFileById(id, false, false);
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

        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new StorageException("Cannot find events");
        }
        return events;
    }

    /**
     * Share/unshare a file
     * @param file File to share or unshare
     * @param share true to share, false otherwise
     * @throws StorageException
     */
    public void changeAccess (GBFile file, boolean share) throws StorageException {

        if (file == null)
            throw new InvalidParameterException("Cannot share/unshare a null file");

        try {
            if(share) {

                sharingTable.create(new Sharing(file));
            } else {

                DeleteBuilder<Sharing, Long> stmt = sharingTable.deleteBuilder();
                stmt.where().eq("file_ID", file.getID());
                if(stmt.delete() < 0)
                    throw new StorageException("Filed is nit shared");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new StorageException(ex.toString());
        }
    }

    /**
     * Return a list of the shared files
     * @return List of all the shared files
     * @throws StorageException
     */
    public List<GBFile> getSharedFiles () throws StorageException {

        try {
            QueryBuilder<GBFile, Long> fileQuery = fileTable.queryBuilder();
            QueryBuilder<Sharing, Long> sharingQuery = sharingTable.queryBuilder();
            return fileQuery.join(sharingQuery).query();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new StorageException(ex.toString());
        }
    }

    /**
     * Check if the specified file is shared or not
     * @param file File to check
     * @return Shared or not
     */
    public boolean isShared (GBFile file) throws StorageException {

        if (file == null)
            throw new InvalidParameterException("File is null");

        try {
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM sharing WHERE file_ID = ?");
            stmt.setLong(1, file.getID());
            ResultSet res = stmt.executeQuery();
            boolean exist = res.next();
            res.close();
            return exist;
        } catch (SQLException ex) {
            throw new StorageException("Cannot check if the file is shared");
        }
    }

    /**
     * Query the database and return a list of files that match the request.
     * @param keyword Keyword of the name of the file
     * @param kind The mime of the file
     * @param from This is equals to the sql 'limit by'. Is the index of the file of the complete result list
     *             from which the returned list starts
     * @param n This indicate how long can the list be
     * @return The list of the files. Empty (but not null) if any file match the request
     * @throws StorageException Database connection or query exception
     */
    public List<GBFile> search (String keyword, String kind, long from, long n) throws StorageException {

        try {
            // Build the query
            QueryBuilder<GBFile, Long> stmt = fileTable.queryBuilder();

            stmt.where().like("name", '%' + keyword + '%')
                    .and().like("mime", '%' + kind + '%');

            if(from > 0)
                stmt.offset(from);
            if(n > -1)
                stmt.limit(new Long(n));

            return stmt.query();
        } catch (SQLException ex) {

            throw new StorageException("Cannot search");
        }
    }

    /**
     * Return a list of the recent files in date order
     * @param from Offset of the result list
     * @param size Limit of result
     * @return List of recent files
     */
    public List<GBFile> getRecentFiles (long from, long size) throws StorageException {

        try {

            // Query builder event table
            QueryBuilder<SyncEvent, Long> eventQuery = eventTable.queryBuilder();

            // Query builder file table
            QueryBuilder<GBFile, Long> fileQuery = fileTable.queryBuilder();

            // Make the query
            return fileQuery.join(eventQuery)
                    .having("kind NOT 'REMOVE_FILE'")
                    .orderBy("date", false)
                    .offset(from)
                    .limit(size)
                    .query();
        } catch (SQLException ex) {

            throw new StorageException("Cannot search");
        }
    }

    /**
     * Move a file to/from the trash
     * @param fileToMove File to move
     * @param toTrash To trash/from trash
     * @throws StorageException
     */
    public void moveToTrash (GBFile fileToMove, boolean toTrash) throws StorageException {

        if (fileToMove == null)
            throw new InvalidParameterException("The file to remove cannot be null");

        // Change the flag
        fileToMove.setTrashed(toTrash);

        try {

            // Update the database
            fileTable.update(fileToMove);
        } catch (SQLException ex) {

            throw new StorageException(ex.toString());
        }
    }

    /**
     * Return a list with the trashed files in alphabetic order
     * @param from Offset of the list
     * @param size Size of the result list
     * @return List with the trashed files
     */
    public List<GBFile> getTrashedFiles (long from, long size) throws StorageException {
        try {

            // Prepare the query
            QueryBuilder<GBFile, Long> queryBuilder = fileTable.queryBuilder();

            // Make the query
            return queryBuilder
                    .orderBy("name", false)
                    .where().eq("trashed", false)
                    .query();
        } catch (SQLException ex) {

            throw new StorageException();
        }
    }

    public SyncEvent copyFile(GBFile src, GBFile dst) throws StorageException {

        src = getFileById(src.getID());

        SyncEvent event = null;

        try {
            event = insertFile(dst);
        } catch (Exception ex) {

        }
        if(src.isDirectory())
            for(GBFile child : src.getChildren())
                copyFile(child, new GBFile(child.getName(), dst.getID(), true));

        return event;
    }

    /**
     * Create a new event that says that this file was opened (useful for the recent file list)
     * @param fileToRegister View file
     */
    public void registerView (GBFile fileToRegister) {

        try {

            // Create the new event
            SyncEvent newEvent = new SyncEvent(SyncEvent.EventKind.OPEN_FILE, fileToRegister);

            // Insert into the database
            eventTable.create(newEvent);
        } catch (SQLException ex) {
            // No one cares...
        }
    }
}