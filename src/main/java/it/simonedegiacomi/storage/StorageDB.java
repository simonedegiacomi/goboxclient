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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private Dao<Sharing, Long> sharingTable;

    /**
     * Create a new database and open the connection
     * @param path Path to the database
     * @throws Exception Exception in case there are
     * some problem with the connection or with the
     * initialization. If any exception are trowed,
     * the object shouldn't be used
     */
    public StorageDB(String path) throws StorageException {

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
        db.close();
        connectionSource.close();
        log.info("Database disconnected");
    }

    /**
     * Init the database tables
     * @throws Exception Exception if the creation of
     * the tables fails.
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
    public void findIDByPath (GBFile file) {
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
                if(!res.next())
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
     * Find the path of the file. This method works only if the father ID of the
     * GBFile is set. However, if you call this method and the path field in the GBFile
     * is set, the method 'findIDByPath' will be called.
     * @param file File that doesn't know his path
     */
    public void findPath(GBFile file) throws StorageException {

        if(file.getFatherID() == GBFile.UNKNOWN_ID) {
            findIDByPath(file);
            return ;
        }

        // Create a new temporary list
        List<GBFile> path = new LinkedList<>();

        long fatherId = file.getFatherID();
        while (fatherId != GBFile.ROOT_ID) {
            GBFile node = this.getFileById(fatherId);
            fatherId = node.getFatherID();
            path.add(0, node);
         }

        // Add also the file because the GBFile.setPath wants it
        path.add(file);

        file.setPathByList(path);
    }

    /**
     * Insert a new file in the database
     * @param newFile New file to insert. The object will be
     *                filled with the new information obtained
     *                inserting the data (the ID)
     * @throws Exception Exception throwed during the insertion
     */
    public SyncEvent insertFile (GBFile newFile) throws SQLException {
        // If the file doesn't know his father, let's find his
        if (newFile.getFatherID() == GBFile.UNKNOWN_ID)
            findIDByPath(newFile);

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
        } catch (SQLException ex) {
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
                findIDByPath(updatedFile);

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
                findIDByPath(fileToRemove);

            // Remove from the database
            fileTable.deleteById(fileToRemove.getID());

            if(fileToRemove.isDirectory())
                for(GBFile child : fileToRemove.getChildren())
                        removeFile(child);

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
     * @throws StorageException
     */
    public void findChildrenByFather(GBFile father) throws StorageException {
        try {
            QueryBuilder<GBFile, Long> stmt =  fileTable.queryBuilder();
            stmt.where().eq("father_ID", father.getID());
            stmt.orderBy("name", true);
            father.setChildren(stmt.query());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
        }
    }

    /**
     * Query the database and return the requested file
     * @param id File Id to search in the database
     * @param children If true, insert in the file also his children
     * @return File retrieved from the database
     */
    public GBFile getFileById (long id, boolean path, boolean children) throws StorageException {
        try {
            // Get the GBFile
            GBFile file = fileTable.queryForId(id);

            // If the file is null throw a new exception
            if(file == null)
                throw new StorageException(StorageException.FILE_NOT_FOUND);

            // Get additional information if need
            if(children && file.isDirectory())
                findChildrenByFather(file);
            if(path)
                findPath(file);
            return file;
        } catch (Exception ex) {
            // TODO divide the case when the file is not found and where there is a real problem

            throw new StorageException(StorageException.FILE_NOT_FOUND);
        }
    }

    /**
     * Query the database and return the GBFile. Either the children and
     * path fields of this object will be null
     * @param id ID of the file
     * @return The wrapped GBFile
     */
    public GBFile getFileById (long id) throws StorageException {
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

    public void changeAccess (GBFile file, boolean share) throws StorageException {
        try {
            if(share) {
                sharingTable.create(new Sharing(file));
            } else {
                DeleteBuilder<Sharing, Long> stmt = sharingTable.deleteBuilder();
                stmt.where().eq("file_ID", file.getID());
                if(stmt.delete() < 0)
                    throw new StorageException("Filed is nit shared");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new StorageException(ex.toString());
        }
    }

    public List<GBFile> getSharedFiles () throws StorageException {

        try {
            QueryBuilder<GBFile, Long> fileQuery = fileTable.queryBuilder();
            QueryBuilder<Sharing, Long> sharingQuery = sharingTable.queryBuilder();
            return fileQuery.join(sharingQuery).query();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new StorageException(ex.toString());
        }
    }

    public boolean isShared (GBFile file) {
        boolean exist;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM sharing WHERE file_ID = ?");
            stmt.setLong(1, file.getID());
            ResultSet res = stmt.executeQuery();
            exist = res.next();
            res.close();
        } catch (Exception ex) {
            exist =  false;
        }
        return exist;
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
        } catch (Exception ex) {
            // TODO: catch the exception
            return new LinkedList<>();
        }
    }

    /**
     * Return a list of the recent files in date order
     * @param from Offset of the result list
     * @param size Limit of result
     * @return List of recent files
     */
    public List<GBFile> getRecentFiles (long from, long size) {

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

            return new LinkedList<>();
        }
    }

    /**
     * Return a list with the trashed files in alphabetic order
     * @param from Offset of the list
     * @param size Size of the result list
     * @return List with the trashed files
     */
    public List<GBFile> getTrashedFiles (long from, long size) {
        try {

            // Prepare the query
            QueryBuilder<GBFile, Long> queryBuilder = fileTable.queryBuilder();

            // Make the query
            return queryBuilder
                    .orderBy("name", false)
                    .where().eq("trashed", false)
                    .query();
        } catch (SQLException ex) {

            return new LinkedList<>();
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
}