package it.simonedegiacomi.storage;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
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
public class DAOStorageDB extends StorageDB {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(DAOStorageDB.class.getName());

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
    public DAOStorageDB(String path) throws StorageException {

        // assert that the path is valid
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
    @Override
    public void close () throws SQLException {

        // Asset that the database is connected
        if (db == null)
            throw new IllegalStateException("The database connection is not open");

        db.close();
        connectionSource.close();
        log.info("Database disconnected");
    }

    /**
     * Return the file from the database with this id
     * @param id ID of the file to find
     * @return File, null if not found
     * @throws StorageException
     */
    @Override
    public GBFile getFileByID(long id) throws StorageException {
        try {
            return fileTable.queryForId(id);
        } catch (SQLException ex) {
            throw new StorageException(ex.toString());
        }
    }

    /**
     * Return the file in the database with this path. The object returned from this method contains the name,
     * the creation and last update date and the trash flag. The children and the path are not determined. If you
     * need them, use {@link #findChildren(GBFile)} (or {@link #getChildren(long)}) or {@link #findPath(GBFile)}
     * (or {@link #findPath(GBFile)}.
     * @param path Path of the file
     * @return File, null if not found
     * @throws StorageException
     */
    @Override
    public GBFile getFileByPath(List<GBFile> path) throws StorageException {
        // Assert that the path is valid
        if (path == null)
            throw new InvalidParameterException("path is null");

        try {
            // Prepare the query that is the same each iteration
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM file WHERE father_ID = ? AND name = ?");

            // Start with the only file where i'm sure o know his father, the root
            long fatherIDOfSomeone = GBFile.ROOT_ID;
            GBFile fatherOfSomeone = null;

            // Find the father of every ancestor node
            for(GBFile ancestor : path) {
                ancestor.setFatherID(fatherIDOfSomeone);

                // Query the database
                stmt.setLong(1, fatherIDOfSomeone);
                stmt.setString(2, ancestor.getName());
                ResultSet res = stmt.executeQuery();

                // If there is no result
                if(!res.next())
                    return null;

                fatherIDOfSomeone = res.getLong("ID");
                res.close();
                ancestor.setID(fatherIDOfSomeone);
                fatherOfSomeone = ancestor;
            }

            return fatherOfSomeone;
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot find ID");
        }
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
            fileTable.create(GBFile.ROOT_FILE);
        }

        log.info("Database tables initialized.");
    }

    /**
     * Find the path of the file. This method works only if the father ID of the GBFile is set.
     * @param file File that doesn't know his path
     */
    @Override
    public List<GBFile> getPath (long id) throws StorageException {
        if(id <= 0)
            throw new InvalidParameterException("invalid id");

        // Create a new temporary list
        List<GBFile> path = new LinkedList<>();

        // Until the father is the root
        while (id != GBFile.ROOT_ID) {

            // find the father of the father
            GBFile node = getFileByID(id);
            id = node.getFatherID();

            // Add this father tot he list
            path.add(0, node);
         }

        // Finally add the root
        path.add(0, GBFile.ROOT_FILE);

        return path;
    }

    /**
     * Insert a new file in the database
     * @param newFile New file to insert. The object will be filled with the new information obtained
     *                inserting the data (the ID)
     * @throws Exception Exception thrown during the insertion
     */
    @Override
    public SyncEvent insertFile (GBFile newFile) throws StorageException {

        // Assert that the file is not null
        if (newFile == null)
            throw new InvalidParameterException("File insert a null file");

        // If the file doesn't know his father, let's find his
        if (newFile.getFatherID() == GBFile.UNKNOWN_ID)
            throw new InvalidParameterException("father id not set");

        try {

            // Check if the file already exists
            GBFile old = getFile(newFile);
            if (old != null) {

                // Set the id of the new file
                newFile.setID(old.getID());
                fileTable.update(newFile);

                // Generate an update event
                SyncEvent event = new SyncEvent(SyncEvent.EventKind.UPDATE_FILE, newFile);
                registerEvent(event);
                return event;
            }

            // Insert into the database
            fileTable.create(newFile);

            log.info("New file inserted on the database");

            // Create the SyncEvent to return
            SyncEvent event = new SyncEvent(SyncEvent.EventKind.NEW_FILE, newFile);

            // And add it to the right db table
            registerEvent(event);

            return event;
        } catch (SQLException ex) {
            throw new StorageException("Cannot insert file into the database");
        }
    }

    /**
     * Add a new row in the event table
     * @param event Event to add
     */
    private SyncEvent registerEvent (SyncEvent event) throws StorageException {

        // Assert that the event is not null
        if (event == null)
            throw new InvalidParameterException("Can't add null event");

        try {
            eventTable.create(event);
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot insert event into database");
        }
        return event;
    }

    /**
     * Assert that the file know his id. If the id is unknown the method {@link #getFile(GBFile)} is called
     * @param file File to which ind the id (if needed)
     * @throws StorageException
     */
    private void assertID (GBFile file) throws StorageException {
        if (file == null)
            throw new InvalidParameterException("File can't be null");

        if (file.getID() == GBFile.UNKNOWN_ID)
            file.setID(getFile(file).getID());
    }

    /**
     * Update the information of a file
     * @param updatedFile File to update with the new information
     * @return Generated sync event
     */
    @Override
    public SyncEvent updateFile (GBFile updatedFile) throws StorageException {

        // assert that the file know his ID
        assertID(updatedFile);

        try {
            // If the file doesn't know his id, let's find it
            if(updatedFile.getID() == GBFile.UNKNOWN_ID)
                updatedFile = getFileByPath(updatedFile.getPathAsList());

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
     * Return the list of the children of this file.
     * NOTE that one of the children is in the trash this method doesn't include it!
     * @param ID ID of the father
     * @return List of children.
     * @throws StorageException
     */
    @Override
    public List<GBFile> getChildren(long ID) throws StorageException {
        try {
            return fileTable.queryBuilder()
                    .orderBy("name", true)
                    .where()
                    .eq("father_ID", ID)
                    .and()
                    .eq("trashed", false)
                    .query();
        } catch (SQLException ex) {
            log.warn(ex);
            throw new StorageException("Cannot find children");
        }
    }

    @Override
    public SyncEvent trashFile(GBFile file, boolean toTrash) throws StorageException {

        // assert that the id is valid
        assertID(file);

        try {
            // Change the state
            file.setTrashed(toTrash);

            // Update the db
            fileTable.update(file);
            return registerEvent(new SyncEvent(SyncEvent.EventKind.REMOVE_FILE, file));
        } catch (SQLException ex) {
            throw new StorageException(ex.toString());
        }
    }

    @Override
    public SyncEvent share(GBFile file, boolean share) throws StorageException {
        assertID(file);

        try {
            if(share) {
                sharingTable.create(new Sharing(file));
            } else {
                DeleteBuilder<Sharing, Long> stmt =  sharingTable.deleteBuilder();
                stmt.where().eq("file_ID", file.getID());
                stmt.delete();
            }

            SyncEvent event = new SyncEvent(share ? SyncEvent.EventKind.SHARE_FILE : SyncEvent.EventKind.UNSHARE_FILE, file);
            registerEvent(event);
            return event;
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new StorageException(ex.toString());
        }
    }

    @Override
    public List<GBFile> getSharedList() throws StorageException {
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
     * Remove a file from the database
     * @param fileToRemove File to remove
     * @return The generated sync event
     */
    @Override
    public SyncEvent removeFile (GBFile fileToRemove) throws StorageException {
        assertID(fileToRemove);
        try {

            // Remove from the database
            int rows = fileTable.deleteById(fileToRemove.getID());

            if (rows != 1)
                throw new StorageException("Removed " + rows + " instead of 1");
            log.info("File removed from database");

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
     * Check if the specified file is shared or not
     * @param file File to check
     * @return Shared or not
     */
    @Override
    public boolean isShared (GBFile file) throws StorageException {
        assertID(file);
        try {
            return sharingTable.queryBuilder()
                    .where()
                    .eq("file_ID", file.getID())
                    .query()
                    .size() > 0;
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
    @Override
    public List<GBFile> search (String keyword, String kind, long from, long n) throws StorageException {

        try {
            // Build the query
            QueryBuilder<GBFile, Long> stmt = fileTable.queryBuilder();

            stmt.where().like("name", '%' + keyword + '%')
                    .and().like("mime", '%' + kind + '%');

            if(from > 0)
                stmt.offset(from);
            if(n > 0)
                stmt.limit(new Long(n));

            return stmt.query();
        } catch (SQLException ex) {

            throw new StorageException("Cannot search");
        }
    }

    @Override
    public void addToRecent(GBFile file) throws StorageException {
        if(file == null)
            throw new InvalidParameterException("file is null");

        try {

            // Create the new event
            SyncEvent newEvent = new SyncEvent(SyncEvent.EventKind.OPEN_FILE, file);

            // Insert into the database
            eventTable.create(newEvent);
        } catch (SQLException ex) {
            // No one cares...
        }
    }

    @Override
    public List<SyncEvent> getRecentList(long from, long size) throws StorageException {
        try {

            QueryBuilder<SyncEvent, Long> eventQuery = eventTable.queryBuilder()
                    .orderBy("date", false)
                    .offset(from)
                    .limit(size);

            // Make the query
            return eventQuery.query();
        } catch (SQLException ex) {
            log.warn(ex.toString(), ex);
            throw new StorageException("Cannot search");
        }
    }

    @Override
    public List<GBFile> getTrashList() throws StorageException {
            try {

                // Prepare the query
                QueryBuilder<GBFile, Long> queryBuilder = fileTable.queryBuilder();

                // Make the query
                return queryBuilder
                        .orderBy("name", false)
                        .where().eq("trashed", true)
                        .query();
            } catch (SQLException ex) {

                throw new StorageException("Cannot trash file");
            }
    }

    @Override
    public SyncEvent move (GBFile src, GBFile dst, boolean copy) throws StorageException {
        assertID(src);

        insertFile(dst);
        if(src.isDirectory()) {
            for (GBFile child : src.getChildren()) {
                move(child, new GBFile(child.getName(), dst.getID(), true), copy);
            }
        }

        if (!copy) {
            removeFile(src);
        }

        // Create the sync event
        SyncEvent detailedEvent = new SyncEvent(copy ? SyncEvent.EventKind.FILE_MOVED : SyncEvent.EventKind.FILE_COPIED);
        detailedEvent.setBefore(src);
        detailedEvent.setRelativeFile(dst);

        // Register the event
        registerEvent(detailedEvent);
        return detailedEvent;
    }
}