package it.simonedegiacomi.goboxapi;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class used to create the logic database representation
 * of a file.
 *
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class GBFile {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(GBFile.class.getName());

    /**
     * The ID of the ROOT directory (well.. file)
     */
    public static final long ROOT_ID = 0;
    public static final long UNKNOWN_ID = -1;
    public static final long UNKNOWN_FATHER = UNKNOWN_ID;

    /**
     * Id of the file. Is not final because when the
     * file is created the ID is not knowed, but we now it
     * only when is insterter on the database
     */
    private long ID = UNKNOWN_ID;

    /**
     * Id of the father, 0 in case that the file is in
     * the root
     */
    private long fatherID = UNKNOWN_FATHER;

    /**
     * Indicate if the file is a 'real' file or
     * a directory
     */
    private final boolean isDirectory;

    /**
     * Size of the file in bytes
     */
    private long size;

    /**
     * Name of the file
     */
    private String name;

    /**
     * Date of ht ecration and the last update of the file
     */
    private long creationDate, lastUpdateDate;

    /**
     * Path of the file
     */
    private List<String> path;

    /**
     * Hash of the file
     */
    private HashCode hash;

    /**
     * If the file is wrapped, this refer to the original file
     */
    private File javaFile;

    public GBFile (File file) {
        this.javaFile = file;
        this.name = file.getName();
        this.isDirectory = file.isDirectory();
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            this.size = attrs.size();
            this.creationDate = attrs.creationTime().toMillis();
            this.lastUpdateDate = attrs.lastAccessTime().toMillis();
            this.setPathByString(file.getPath());
        } catch (IOException e) {
            // Trust me, i really care...
            e.printStackTrace();
        }
    }

    public GBFile (JSONObject json) throws GBException {

        try {
            this.name = json.getString("name");

            this.isDirectory = json.getBoolean("isDirectory");
            this.size = json.getLong("size");
            this.creationDate = json.getLong("creation");

            this.lastUpdateDate = json.getLong("lastUpdate");
            this.ID = json.has("id") ? json.getLong("id") : UNKNOWN_ID;
            if (json.has("fatherId"))
                this.fatherID = json.getLong("fatherId");
            else if (json.has("path"))
                this.setPathByString(json.getString("path"));
            else
                this.fatherID = UNKNOWN_FATHER;
        } catch (JSONException ex) {
            throw new GBException(ex.toString());
        }
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public GBFile(String name, long fatherID, boolean isDirectory) {
        this.name = name;
        this.fatherID = fatherID;
        this.isDirectory = isDirectory;
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param ID ID of the file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public GBFile(long ID, long fatherID, String name, boolean isDirectory) {
        this.ID = ID;
        this.fatherID = fatherID;
        this.name = name;
        this.isDirectory = isDirectory;
    }

    /**
     * Return the ID of the file
     * @return The ID of the file
     */
    public long getID() {
        return ID;
    }

    /**
     * Set the ID of the file.
     * Only the classes inside this package, or that
     * extends this class should edit the ID of the file
     * @param ID ID of the file
     */
    public void setID(long ID) {
        this.ID = ID;
    }

    /**
     * Set the ID of the father.
     * Only the classes inside this package, or that
     * extends this class should edit the ID of the file
     * @param fatherID ID of the father
     */
    public void setFatherID(long fatherID) {
        this.fatherID = ID;
    }


    /**
     * Return the ID of the file
     * @return ID of the father
     */
    public long getFatherID() {
        return fatherID;
    }

    /**
     * Return the size of the file expressed in bytes
     * @return Size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Set the size of the file. If is called, is a good idea
     * to change also the lasyUpdateDate
     * @param size The size of the file
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Return the name of the file
     * @return Name of the file
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return true if the file is a directory, false otherwise
     * @return Identity of the file
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(long lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public JSONObject toJSON () {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", name);
            obj.put("id", ID);
            obj.put("isDirectory", isDirectory);
            obj.put("creation", creationDate);
            obj.put("lastUpdate", lastUpdateDate);
            obj.put("size", size);
            if(fatherID != UNKNOWN_FATHER)
                obj.put("fatherId", fatherID);
            else
                obj.put("path", getPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("Ho creato il json " + obj.toString());
        return obj;
    }

    /**
     * Return the hash of the file if the file was wrapped from a File and
     * if is not an directory
     * @return
     */
    public HashCode getHash () throws IOException {
        if (hash == null && !isDirectory && javaFile != null)
            hash = com.google.common.io.Files.hash(javaFile, Hashing.md5());
        return hash;
    }

    /**
     * Generate a new GBFile, with his fatherID equals to this id
     * @param name Name of the new file
     * @param isDirectory is a directory or a  file?
     * @return The new file
     */
    public GBFile generateChild (String name, boolean isDirectory) {
        return new GBFile(name, ID, isDirectory);
    }

    /**
     * Return the path of the file as a string
     * @return String that rappresentate the path of the file
     */
    public String getPath () {
        StringBuilder builder = new StringBuilder();
        for(String piece : path)
            builder.append(piece);
        return builder.toString();
    }

    /**
     * Return the Path as a list of 'piece'. Each 'piece' is a
     * folder.
     * @return List rappresentation of the path
     */
    public List<String> getListPath() {
        return path;
    }

    /**
     * Set the path of the file, without updating the fatherID
     * @param pieces New path of the file.
     */
    public void setPath (List<String> pieces) {
        this.path = pieces;
    }

    /**
     * Set the new path of the file without updating the fatherID
     * @param str String that contains the path
     */
    public void setPathByString (String str) {
        this.path = new LinkedList<>();
        for(String piece : str.split("/"))
            path.add(piece);
    }

    /**
     * Return the java io File reference to this file
     * @return Reference to this file
     */
    public File toFile () {
        if (javaFile == null)
            javaFile = new File(getPath());
        return javaFile;
    }

    /**
     * Return the Path object of the file
     * @return Path reference to this file
     */
    public Path toPath () {
        return toFile().toPath();
    }

    @Override
    public String toString() {
        return "GBFile{" +
                "ID=" + ID +
                ", fatherID=" + fatherID +
                ", isDirectory=" + isDirectory +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", lastUpdateDate='" + lastUpdateDate + '\'' +
                '}';
    }
}