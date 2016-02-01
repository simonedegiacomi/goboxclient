package it.simonedegiacomi.storage;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class wrap the database in a object that
 * exposes all the method used in the GoBox Storage.
 *
 * Created by Degiacomi Simone on 23/12/2015
 */
public class StorageDB {

    private static final Logger log = Logger.getLogger(StorageDB.class.getName());

    /**
     * Connection to the database
     */
    private Connection db;

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
        db = DriverManager.getConnection("jdbc:sqlite:" + path);
        log.info("Connected to local SQLite database");
        try {
            // Initialize the tables
            initDatabase();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            // If there is any exception with the initialization
            // close the connection
            db.close();
        }
        db.setAutoCommit(true);
    }

    /**
     * Close the connection
     * @throws SQLException
     */
    public void close () throws SQLException {
        db.close();
        log.info("Database disconnected");
    }

    /**
     * Init the database tables
     * @throws Exception Exception if the creation of
     * the tables fails.
     */
    private void initDatabase () throws Exception {
        log.fine("The database is empty");
        // Prepare the statement
        Statement stmt = db.createStatement();

        // Create the file table
        stmt.execute("CREATE TABLE IF NOT EXISTS file (" +
                "ID integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "name varchar(255) not null," +
                "father_ID integer," +
                "hash byte(20)," +
                "is_directory tinyint not null," +
                "size integer," +
                "creation date," +
                "last_update date)");

        // Create the file table
        stmt.execute("CREATE TABLE IF NOT EXISTS event (" +
                "ID integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "file_ID integer not null," +
                "kind text not null," +
                "date integer)");

        // Check if the root file is already in the database
        ResultSet res = stmt.executeQuery("SELECT ID FROM file WHERE ID = 0");
        boolean exist = res.next();
        res.close();
        if(!exist)
            stmt.execute("INSERT INTO file (ID, name, is_directory) VALUES (0, 'Root', 1)");

        // And commit the changes
        if (!db.getAutoCommit())
            db.commit();

        log.fine("Database tables initialized.");
    }

    /**
     * Find the father of the file and set it in the file object
     * @param file File that doesn't know who is his father
     */
    public void findFather (GBFile file) {
        // Just need to find the father, so get the path in a list
        List<GBFile> path = file.getPathAsList();

        if(path.size() <= 1) {
            // This means that the father is the root
            file.setFatherID(GBFile.ROOT_ID);
            return ;
        }

        // Otherwise query the database
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM file WHERE father_ID = ? AND name = ?");

            long fatherOfSomeone = GBFile.ROOT_ID;

            Iterator<GBFile> it = path.iterator();
            while(it.hasNext()) {
                GBFile ancestor = it.next();
                if(!it.hasNext()) // If it is the last
                    break;
                stmt.setLong(1, fatherOfSomeone);
                stmt.setString(2, ancestor.getName());
                System.out.printf("Searching the ID of %s which father is %d\n", ancestor.getName(), fatherOfSomeone);
                ResultSet res = stmt.executeQuery();
                fatherOfSomeone = res.getLong("ID");
                res.close();
                ancestor.setFatherID(fatherOfSomeone); // mmmm... we have it however
            }

            file.setFatherID(fatherOfSomeone);
            System.out.printf("The father of %s is %d!\n", file.getName(), fatherOfSomeone);
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
     * Create a new array that contains all the folder from the root to the
     * file (or directory) as argument
     * @param file File that doesn't know his path
     * @return Array of GBFiles that are the nodes in hierarchy order
     */
    public List<GBFile> findPathAsList(GBFile file) {

        // Create a new temporary list
        LinkedList<GBFile> path = new LinkedList<>();

        // §add int he first position the file
        path.add(file);

        long fatherId = file.getFatherID();
        while (fatherId != GBFile.ROOT_ID) {
            GBFile node = this.getFileById(fatherId);
            fatherId = node.getFatherID();
            path.add(0, node);
        }

        GBFile nodes[] = new GBFile[path.size()];
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
        // If the file doesn't know his father, let's find him
        if (newFile.getFatherID() == GBFile.UNKNOWN_FATHER)
            findFather(newFile);
        // If the file doesn't know his pathString, let's find it
        if(newFile.getPathAsList() == null)
            findPath(newFile);
        // Prepare the statement to insert the file
        PreparedStatement stmt = db.prepareStatement("INSERT INTO FILE" +
                "(name, father_ID, is_directory, creation, last_update)" +
                "VALUES (?, ?, ?, ?, ?)");

        // Bind the new data
        stmt.setString(1, newFile.getName());
        stmt.setLong(2, newFile.getFatherID());
        stmt.setBoolean(3, newFile.isDirectory());
        stmt.setLong(4, newFile.getCreationDate());
        stmt.setLong(5, newFile.getLastUpdateDate());

        // Execute the update
        stmt.executeUpdate();

        // Get the ID of the inserted file
        long lastInsertedId = stmt.getGeneratedKeys().getLong(1);

        // Set the ID in the file
        newFile.setID(lastInsertedId);

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
    private void registerEvent (SyncEvent event) {
        try {
            PreparedStatement stmt = db.prepareStatement("INSERT INTO event (file_ID, kind, date) VALUES (?,?,?)");
            stmt.setLong(1, event.getRelativeFile().getID());
            stmt.setString(2, event.getKindAsString());
            // For now set this date
            // TODO: implement real date
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
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
            PreparedStatement stmt = db.prepareStatement("UPDATE file WHERE ID = ?" +
                    "SET name = ? SET last_update = ? SET size = ?");
            stmt.setLong(1, updatedFile.getID());
            stmt.setString(2, updatedFile.getName());
            stmt.setLong(3, updatedFile.getLastUpdateDate());
            stmt.setLong(4, updatedFile.getSize());

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
            PreparedStatement stmt = db.prepareStatement("DELETE FROM file WHERE ID = ?");
            stmt.setLong(1, fileToRemove.getID());
            stmt.executeUpdate();

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
     * Query the database to find the children of a directory
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
            // Create the sql statement
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE ID = ?");
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            GBFile file = new GBFile (res.getLong("ID"), res.getLong("father_ID"), res.getString("name"), res.getBoolean("is_directory"));
            file.setSize(res.getLong("size"));
            if(file.isDirectory() && children)
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
}