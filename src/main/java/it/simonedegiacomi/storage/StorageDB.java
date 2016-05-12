package it.simonedegiacomi.storage;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface of the database used by the storage
 * Created on 22/04/16.
 * @author Degiacomi Simone
 */
public abstract class StorageDB {

    /**
     * Close the connection to the database
     * @throws SQLException
     */
    public abstract void close () throws SQLException;

    /**
     * Query the database and return the file that has the specified id. If the file
     * with the specified id is not found, a null pointer will be returned
     * @param id ID of the file to find
     * @return File with this ID or null if not found
     * @throws StorageException Exception thrown with the query. If the exception is
     * thrown, this doesn't mean that the file doesn't exist
     */
    public abstract GBFile getFileByID (long id) throws StorageException;

    /**
     * Return the file that has the specified path. If no files are found, return null
     * TODO: Add a static method in the  GBFile that transform the String to the list
     * @param path Path of the file
     * @return File with this path or null
     * @throws Exception thrown with the query. If the exception is thrown, this
     * doesn't mean that the file doesn't exist
     */
    public abstract GBFile getFileByPath (List<GBFile> path) throws StorageException;

    /**
     * This method is just an alias for {@link #getFileByID(long)} or {@link #getFileByPath(List)}
     * @param file File with few information
     * @return File or null if the file is not in the database
     * @throws StorageException
     */
    public GBFile getFile (GBFile file) throws StorageException {

        // Try with the id
        if (file.getID() != GBFile.UNKNOWN_ID)
            return getFileByID(file.getID());

        // Try with the path
        if (file.getPathAsList() != null)
            return getFileByPath(file.getPathAsList());

        // I'm sorry
        return null;
    }

    /**
     * This method call {@link #getFile(GBFile)} and then find the children and the path (if needed)
     * @param file File to find
     * @param path Find the path?
     * @param children Find the children ?
     * @return File with the request information
     * @throws StorageException
     */
    public GBFile getFile (GBFile file, boolean path, boolean children) throws StorageException {
        GBFile dbFile = getFile(file);
        if (dbFile != null) {
            if (children)
                findChildren(dbFile);
            if (path)
                findPath(dbFile);
        }
        return dbFile;
    }

    /**
     * Find the path given the file ID. This return the list in the right format to call the setPath GBFile
     * method. If the file with this ID doesn't exist, return null
     * @param ID of the file
     * @return Path of the file
     * @throws StorageException
     */
    public abstract List<GBFile> getPath (long ID) throws StorageException;

    /**
     * Calls {@link #getPath(long)} and then sets the path to the specified file
     * @param file
     * @throws StorageException
     */
    public void findPath (GBFile file) throws StorageException {
        file.setPathByList(getPath(file.getID()));
    }

    /**
     * This method insert the file into the database. The new ID of the file is set in the file passed as argument.
     * @param fileToInsert File to insert in the database
     * @return Generated event
     * @throws StorageException
     */
    public abstract SyncEvent insertFile (GBFile fileToInsert) throws StorageException;

    /**
     * Update the name and dates of the specified file.
     * @param fileToUpdate File to update
     * @return Generated sync event
     * @throws StorageException
     */
    public abstract SyncEvent updateFile (GBFile fileToUpdate) throws StorageException;

    /**
     * Return the list of the file's children. If the file doesn't exist of isn't a folder, return null
     * @param ID ID of the father
     * @return List of child
     * @throws StorageException
     */
    public abstract List<GBFile> getChildren (long ID) throws StorageException;

    /**
     * Calls {@link #getChildren(long)} and assign the result list to the specified file
     * @param file File to find the children list
     * @throws StorageException
     */
    public void findChildren (GBFile file) throws StorageException {
        file.setChildren(getChildren(file.getID()));
    }

    /**
     * Move the file to/from the trash.
     * @param ID ID of the file to move
     * @param toTrash To the trash or recover from trash
     * @return Generated event
     * @throws StorageException
     */
    public abstract SyncEvent trashFile (GBFile file, boolean toTrash) throws StorageException;

    /**
     * Trash the specified file reading the trash property of the specified object
     * @param file File to move to/from the trash
     * @return generated event
     * @throws StorageException
     */
    public SyncEvent trashFile (GBFile file) throws StorageException {
        return trashFile(file, file.isTrashed());
    }

    /**
     * Return the list of the trashed files
     * @return List of the trashed files
     * @throws StorageException
     */
    public abstract List<GBFile> getTrashList () throws StorageException;

    /**
     * Delete the specified file, even if the file wasn't in the trash
     * @param ID File to remove
     * @return Generated event
     * @throws StorageException
     */
    public abstract SyncEvent removeFile (GBFile file) throws StorageException;

    /**
     * Move, Rename or copy the file
     * @param src Source file
     * @param dst Destination file (this must have a valid father id)
     * @param cut True to copy the file, false to move
     * @return Generate event
     * @throws StorageException
     */
    public abstract SyncEvent move (GBFile src, GBFile dst, boolean copy) throws StorageException;

    /**
     * Share (or stop sharing) a file or a folder.
     * @param file File
     * @param share True to share, False to stop sharing
     * @throws StorageException
     */
    public abstract SyncEvent share (GBFile file, boolean share) throws StorageException;

    /**
     * Check if the specified file is shared or not, If a file with this ID doesn't exist, return false
     * @param file file
     * @return True if shared, false if not shared or if the file doesn't exist
     * @throws StorageException
     */
    public abstract boolean isShared (GBFile file) throws StorageException;

    /**
     * Return a list with all the shared files
     * @return List with the shared files
     * @throws StorageException
     */
    public abstract List<GBFile> getSharedList () throws StorageException;

    /**
     * Search and return the found files in alphabetic order
     * @param keyword Keyword that must be present in the file name
     * @param kind Kind (mime) of file. Set this field to null to search for any kind of file
     * @param from From which file the list starts. Set this to -1 to get all the files
     * @param size Size of the result list. Set this to -1 to get all the found files
     * @return List of matching files
     * @throws StorageException
     */
    public abstract List<GBFile> search (String keyword, String kind, long from, long size) throws StorageException;

    /**
     * Add the specified file to the recent file list
     * @param ID File to add
     * @throws StorageException
     */
    public abstract void addToRecent (GBFile file) throws StorageException;

    /**
     * Return the list with the recent files in order.
     * @param from From which file the result list must starts. If this is higher than the number of record, an
     *             empty (not null) list will be returned.
     * @param size Max size of the result list
     * @return List of recent files
     * @throws StorageException
     */
    public abstract List<SyncEvent> getRecentList (long from, long size) throws StorageException;
}