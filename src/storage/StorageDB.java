package storage;

import goboxapi.GBFile;
import goboxapi.client.Sync;
import goboxapi.client.SyncEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


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
            // If there is any exeption with the initialization
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
                "creation date not null," +
                "last_update date not null)");

        // Create the file table
        stmt.execute("CREATE TABLE IF NOT EXISTS event (" +
                "ID integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "file_ID integer not null," +
                "kind integer not null," +
                "date integer)");

        // And commit the changes
        if (!db.getAutoCommit())
            db.commit();

        log.fine("Database tables initialized.");
    }

    /**
     * Find the pather of the
     * @param file
     */
    public void findFather (GBFile file) {
        // Just need to find the father, so get the path in a list
        List<String> path = file.getListPath();

        // In case of the file is in the root
        if (path.size() <= 1) {

            // Set the root id father
            file.setFatherID(GBFile.ROOT_ID);
            return ;
        }

        // Otherwise query the database
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT ID FROM file WHERE name = ?");
            stmt.setString(1, path.get(path.size() - 2));
            file.setFatherID(stmt.executeQuery().getLong("ID"));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    public void findPath (GBFile file) {
        LinkedList<String> path = new LinkedList<>();
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT name, father_ID FROM file WHERE ID = ?");
            long fatherId = file.getFatherID();
            while (fatherId != GBFile.ROOT_ID) {
                stmt.setLong(1, fatherId);
                ResultSet res = stmt.executeQuery();
                fatherId = res.getLong("father_ID");
                path.add(0, res.getString("name"));
                res.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        if (newFile.getFatherID() == GBFile.UNKNOW_FATHER)
            findFather(newFile);
        // If the file doesn't know his pathString, let's find it
        if(newFile.getPath() == null)
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
        SyncEvent event = new SyncEvent(SyncEvent.CREATE_FILE, newFile);

        // And add it to the right db table
        registerEvent(event);

        return event;
    }

    private void registerEvent (SyncEvent event) {
        try {
            PreparedStatement stmt = db.prepareStatement("INSERT INTO event (file_ID, kind, date) VALUES (?,?,?)");
            stmt.setLong(1, event.getRelativeFile().getID());
            stmt.setInt(2, event.getKind());
            // For now set this date
            // TODO: implement real date
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    public SyncEvent updateFile (GBFile updatedFile) {
        try {
            PreparedStatement stmt = db.prepareStatement("UPDATE file WHERE ID = ?" +
                    "SET name = ? SET last_update = ? SET size = ?");
            stmt.setLong(1, updatedFile.getID());
            stmt.setString(2, updatedFile.getName());
            stmt.setLong(3, updatedFile.getLastUpdateDate());
            stmt.setLong(4, updatedFile.getSize());

            // Create sync event
            SyncEvent event = new SyncEvent(SyncEvent.EDIT_FILE, updatedFile);
            registerEvent(event);
            return  event;
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        return null;
    }

    public SyncEvent removeFile (GBFile fileToRemove) {
        try {
            PreparedStatement stmt = db.prepareStatement("DELETE FROM file WHERE ID = ?");
            stmt.setLong(1, fileToRemove.getID());
            stmt.executeUpdate();

            // Create the sync event
            SyncEvent event = new SyncEvent(SyncEvent.REMOVE_FILE, fileToRemove);
            registerEvent(event);
            return event;
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
        return null;
    }

    public GBFile[] getChildrenByFather (long fatherID) throws StorageException {
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE father_ID = ? ORDER BY name");
            stmt.setLong(1, fatherID);
            ResultSet sqlResults = stmt.executeQuery();
            ArrayList<GBFile> results = new ArrayList<>();
            while(sqlResults.next()) {
                GBFile temp = new GBFile(sqlResults.getInt("ID"), fatherID,
                        sqlResults.getString("name"), sqlResults.getBoolean("is_directory"));
                temp.setSize(sqlResults.getLong("size"));
                temp.setCreationDate(sqlResults.getLong("creation"));
                temp.setLastUpdateDate(sqlResults.getLong("last_update"));
                results.add(temp);
            }

            return results.toArray(new GBFile[results.size()]);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            return null;
        }
    }

    public GBFile getFileById (long id) {
        try {
            if (id == 0) {
                return new GBFile("root", 0 ,true);
            }
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE ID = ?");
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            GBFile file = new GBFile (res.getLong("ID"), res.getLong("father_ID"), res.getString("name"), res.getBoolean("is_directory"));
            file.setSize(res.getLong("size"));
            return file;
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            return null;
        }
    }
}