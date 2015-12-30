package mydb;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MyDB {

    private static final Logger log = Logger.getLogger(MyDB.class.getName());
    private final Connection db;

    /**
     * Create a new database and open the connection
     * @param path Path to the database
     * @throws Exception Exception in case there are
     * some problem with the connection or with the
     * initialization. If any exception are trowed,
     * the object shouldn't be used
     */
    public MyDB (String path) throws Exception {
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
                "father_ID int unsigned," +
                "is_directory tinyint not null," +
                "hide tinyint not null DEFAULT 0," +
                "size int unsigned," +
                "creation date not null," +
                "last_update date not null)");

        // And commit the changes
        if (!db.getAutoCommit())
            db.commit();
        log.fine("Database tables created.");
    }

    /**
     * Insert a new file in the database
     * @param newFile New file to insert. The object will be
     *                filled with the new information obtained
     *                inserting the data (the ID)
     * @throws Exception Exception throwed during the insertion
     */
    public void insertFile (DBFile newFile) throws Exception {
        // Prepare the statement
        PreparedStatement stmt = db.prepareStatement("INSERT INTO FILE" +
                "(name, father_ID, is_directory, creation, last_update)" +
                "VALUES (?, ?, ?, ?, ?)");

        // Bind the new data
        stmt.setString(1, newFile.getName());
        stmt.setLong(2, newFile.getFatherID());
        stmt.setBoolean(3, newFile.isDirectory());
        stmt.setDate(4, newFile.getCreationDate());
        stmt.setDate(5, newFile.getLastUpdateDate());

        // Execute the update
        stmt.executeUpdate();

        // Get the ID of the inserted file
        long lastInsertedId = stmt.getGeneratedKeys().getLong(1);

        // Set the ID in the file
        newFile.setID(lastInsertedId);

        log.info("New file inserted on the database");
    }

    public DBFile[] getChildrenByFather (long fatherID) throws NotDirectoryException {
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE father_ID = ? ORDER BY name");
            stmt.setLong(1, fatherID);
            ResultSet sqlResults = stmt.executeQuery();
            ArrayList<DBFile> results = new ArrayList<>();
            while(sqlResults.next()) {
                DBFile temp = new DBFile(sqlResults.getInt("ID"), fatherID,
                        sqlResults.getString("name"), sqlResults.getBoolean("is_directory"));
                temp.setSize(sqlResults.getLong("size"));
                temp.setCreationDate(sqlResults.getDate("creation"));
                temp.setLastUpdateDate(sqlResults.getDate("last_update"));
                results.add(temp);
            }

            return results.toArray(new DBFile[results.size()]);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            return null;
        }
    }

    public DBFile getFileById (long id) {
        try {
            if (id == 0) {
                return new DBFile("root", 0 ,true);
            }
            PreparedStatement stmt = db.prepareStatement("SELECT * FROM file WHERE ID = ?");
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            DBFile file = new DBFile (res.getLong("ID"), res.getLong("father_ID"), res.getString("name"), res.getBoolean("is_directory"));
            file.setSize(res.getLong("size"));
            return file;
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.toString(), ex);
            return null;
        }
    }
}