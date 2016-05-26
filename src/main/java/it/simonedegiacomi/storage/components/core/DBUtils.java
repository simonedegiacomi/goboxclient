package it.simonedegiacomi.storage.components.core;

import com.j256.ormlite.dao.Dao;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.StorageException;

import java.sql.SQLException;
import java.util.List;

/**
 * Created on 26/05/16.
 * @author Degiacomi Simone
 */
public class DBUtils {

    /**
     * Return the database reference of the file.
     * This method tries with:
     * - The ID, using {@link #getFileById(Dao, long)}
     * - The combination of fatherID and name
     * - The path
     *
     * This method return a file without the path and the children list.
     * @param table Database table
     * @param file File to search
     * @return Database reference
     * @throws SQLException Exception while querying the database
     */
    public static GBFile getFile (Dao<GBFile, Long> table, GBFile file) throws SQLException {

        // Try with the id
        if (file.getID() != GBFile.UNKNOWN_ID) {
            GBFile dbFile = getFileById(table, file);
            if (dbFile != null) {
                return dbFile;
            }
        }

        // Try with the father and the name
        if (file.getFatherID() != GBFile.UNKNOWN_ID && file.getName() != null) {
            GBFile dbFile = getFileByFatherAndName(table, file.getFatherID(), file.getName());
            if (dbFile != null) {
                return dbFile;
            }
        }

        // Try with the path
        List<GBFile> path = file.getPathAsList();
        if (path != null) {
            GBFile dbFile = getFileByPath(table, path);
            if (dbFile != null) {
                return dbFile;
            }
        }

        // Sorry, not found
        return null;
    }

    /**
     * Return the file in the database table with the specified id. If the file doesn't exists, null is returned
     * This method return a file without the path and the children list.
     * @param id ID of the file
     * @return File with the specified id
     * @throws SQLException Exception while querying the database
     */
    public static GBFile getFileById (Dao<GBFile, Long> table, long id) throws SQLException {
        return table.queryForId(id);
    }

    /**
     * Search for the file with the specified path
     * This method return a file without the path and the children list.
     * @param path Path of the file
     * @return Found file, null the file is not found
     * @throws StorageException Exception while querying the database
     */
    public static GBFile getFileByPath (Dao<GBFile, Long> table, List<GBFile> path) throws SQLException {

        // Start with the only file where i'm sure o know his father, the root
        long fatherIDOfSomeone = GBFile.ROOT_ID;
        GBFile fatherOfSomeone = GBFile.ROOT_FILE;

        // Find the father of every ancestor node
        for(GBFile ancestor : path) {
            ancestor.setFatherID(fatherIDOfSomeone);

            // Query the database
            GBFile res = table.queryBuilder()
                    .where()
                    .eq("fatherID", fatherIDOfSomeone)
                    .and()
                    .eq("name", ancestor.getName())
                    .queryForFirst();

            if (res == null) {
                return null;
            }

            fatherIDOfSomeone = res.getID();
            ancestor.setID(fatherIDOfSomeone);
            fatherOfSomeone = ancestor;
        }

        return fatherOfSomeone;
    }

    /**
     * Search a file given the father id and the name
     * This method return a file without the path and the children list.
     * @param table Database table
     * @param fatherID Father id
     * @param childName Child name
     * @return Foudn file, nul if the file doesn't exists
     * @throws SQLException exception while querying the database
     */
    public static GBFile getFileByFatherAndName (Dao<GBFile, Long> table, long fatherID, String childName) throws SQLException {

        // Find the father
        GBFile father = getFileById(fatherID);

        if (father == null) {
            return null;
        }

        for (GBFile child : father.getChildren()) {
            if (child.getName().equals(childName)) {
                return table.queryForId(child.getID());
            }
        }

        return null;
    }

    /**
     * Check if the specified file exists in the database table. For more details, see {@link #getFile(Dao, GBFile)}
     * @param table Database table
     * @param file File to search
     * @return True if the file exists, false otherwise
     * @throws SQLException Exception while querying the database
     */
    public static boolean exists (Dao<GBFile, Long> table, GBFile file) throws SQLException {
        return getFile(table, file) != null;
    }
}