package it.simonedegiacomi.storage.components.core.utils;

import com.j256.ormlite.dao.Dao;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.Sharing;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.storage.StorageException;

import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 26/05/16.
 * @author Degiacomi Simone
 */
public class DBCommonUtils {

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
            GBFile dbFile = getFileById(table, file.getID());
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
        GBFile father = getFileById(table, fatherID);

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

    /**
     * Check if the file is shared or not. This method is just an alias for {@link #getSharingByFileId(Dao, long)}.
     * @param shareTable Database sharing table
     * @param dbFile File to check
     * @return True if shared, false otherwise
     * @throws SQLException Error while querying the database
     */
    public static boolean isFileSharedByFileId (Dao<Sharing, Long> shareTable, long fileId) throws SQLException {
        return getSharingByFileId(shareTable, fileId) != null;
    }

    /**
     * Find the sharing with the specified file id.
     * @param shareTable Database table
     * @param fileId ID of the shared file
     * @return Sharing instance
     * @throws SQLException Error while querying the database
     */
    public static Sharing getSharingByFileId(Dao<Sharing, Long> shareTable, long fileId) throws SQLException {
        return shareTable.queryBuilder()
                .where()
                .eq("file_ID", fileId)
                .queryForFirst();
    }

    /**
     * Find the children of the specified file and set it to the file.
     * NOTE that this method work only if the file knows his Id
     * @param fileTable Database table. This file must know his Id.
     * @param dbFile Father file to fill with children
     * @throws SQLException Error while querying the database
     */
    public static void findChildren(Dao<GBFile, Long> fileTable, GBFile dbFile) throws SQLException {

        // assert tha the file knows his id
        if (dbFile.getID() == GBFile.UNKNOWN_ID)
            throw new InvalidParameterException("The file doesn't know his Id");

        dbFile.setChildren(fileTable.queryBuilder()
                .orderBy("name", true)
                .where()
                .eq("father_ID", dbFile.getID())
                .and()
                .eq("trashed", false)
                .query());
    }

    /**
     * Find the path of the specified file.
     * NOTE that the specified file must know his id
     * @param fileTable Database table
     * @param dbFile File to fill with the path. This file must know his id
     * @throws SQLException Error while querying the database
     */
    public static void findPath(Dao<GBFile, Long> fileTable, GBFile dbFile) throws SQLException {

        // assert that the file knows his id
        if(dbFile.getID() == GBFile.UNKNOWN_ID)
            throw new InvalidParameterException("File doesn't know his id'");

        // Create a new temporary list
        List<GBFile> path = new LinkedList<>();

        long id = dbFile.getID();

        // Until the father is the root
        while (id != GBFile.ROOT_ID) {

            // find the father of the father
            GBFile node = getFileById(fileTable, id);
            id = node.getFatherID();

            // Add this father tot he list
            path.add(0, node);
        }

        // Finally add the root
        path.add(0, GBFile.ROOT_FILE);

        // and set the path to the file
        dbFile.setPathByList(path);
    }
}