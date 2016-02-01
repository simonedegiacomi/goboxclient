package it.simonedegiacomi.goboxapi;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
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
    private boolean isDirectory;

    /**
     * Size of the file in bytes
     */
    private long size;

    /**
     * Name of the file
     */
    private String name;

    /**
     * Date of ht creation and the last update of the file
     */
    private long creationDate, lastUpdateDate;

    /**
     * Path of the file
     * This path doesn't contains this file as last file, because Gson doesn't like
     * this... so i need to add the file every time the getPath method is called
     */
    private List<GBFile> path;

    /**
     * Hash of the file
     */
    private HashCode hash;

    /**
     * If the file is wrapped, this refer to the original file.
     * This as a Gson name starting iht the underscore because this object make sense
     * only in this FileSystem
     */
    private File javaFile;

    /**
     * List of children of this file
     */
    private List<GBFile> children;

    /**
     * Create a new GBFile starting only with the name and the type of file (file or
     * folder). All the other fields are null
     * @param name Name of the file
     * @param isDirectory Type of file (folder or file)
     */
    public GBFile (String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
    }

    /**
     * Create a new GBFile from a java file and a path prefix. This path prefix will
     * be removed from the path obtained from the java file
     * @param file Java file representation of the file
     * @param prefix Prefix to remove from the path
     */
    public GBFile (File file, String prefix) {
        this.javaFile = file;
        this.name = file.getName();
        this.isDirectory = file.isDirectory();
        this.setPathByString(file.getPath(), prefix);
        try {
            loadAttributes();
        } catch (IOException e) {
            // Trust me, i really care...
            e.printStackTrace();
        }
    }

    /**
     * This method loads the attributes of the file.
     */
    public void loadAttributes ()  throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(javaFile.toPath(), BasicFileAttributes.class);
        this.size = attrs.size();
        this.creationDate = attrs.creationTime().toMillis();
        this.lastUpdateDate = attrs.lastAccessTime().toMillis();
    }

    /**
     * Create a new file starting from the java representation. This method work just like
     * the (file, prefix) and doesn't remove anything from the path, so be careful!
     * @param file Java representation of the file
     */
    public GBFile (File file) {
        this(file, null);
    }

    /**
     * Create a new GBFile from a JSON that represent this file in another
     * client or in the storage.
     * @param json JSON with teh information of the file
     * @throws GBException throwed if the json object is not corrected
     * @Deprecated Use Gson instead
     */
    public GBFile (JSONObject json) throws GBException {

        try {
            this.name = json.getString("name");

            this.isDirectory = json.optBoolean("isDirectory");
            this.size = json.optLong("size");
            this.creationDate = json.optLong("creation");

            this.lastUpdateDate = json.optLong("lastUpdate");
            this.ID = json.has("ID") ? json.getLong("ID") : UNKNOWN_ID;
            if (json.has("fatherID") && json.get("fatherID").toString().length() > 0)
                this.fatherID = json.getLong("fatherID");
            else if (json.has("path") && json.getString("path").length() > 0)
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
    public void setFatherID(long newFatherID) {
        this.fatherID = newFatherID;
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

    /**
     * Return the date if the last update of this file
     * @return Date expressed in milliseconds
     */
    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * Set the last update of this file.
     * @param lastUpdateDate date of the last update in
     *                       milliseconds
     */
    public void setLastUpdateDate(long lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * Return the date of the creation of this file
     * @return Date of the creation of this file in milliseconds
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Set the creation date
     * @param creationDate Date of the creation in milliseconds
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public JSONObject toJSON () {
        try {
            System.out.println("Path is " + path + " and gson " + new Gson().toJson(this));
            return new JSONObject(new Gson().toJson(this));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Return the hash of the file if the file was wrapped from a File and
     * if is not an directory.
     * This method will block the thread until the hash is computed
     * @return The hash of the file
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
     * @return The new generated file, son of this file
     */
    public GBFile generateChild (String name, boolean isDirectory) {
        return new GBFile(name, ID, isDirectory);
    }

    /**
     * Return the path of the file as a string. This path contains this file as last
     * node and the specified prefix passed as argument
     * @param prefix Prefix to add to the generated path
     * @return String path of the file
     */
    public String getPathAsString(String prefix) {
        StringBuilder builder = new StringBuilder();
        if(prefix != null)
            builder.append(prefix);
        for(GBFile piece : getPathAsList())
            builder.append('/').append(piece.getName());
        return builder.toString();
    }

    /**
     * Return the path of the file as a string. This path include this file ad last node
     * @return String path of the file
     */
    public String getPathAsString() {
        return getPathAsString(null);
    }

    /**
     * Return the Path as a list of 'piece'. The last piece is this file
     * @return List representation of the path including this file
     */
    public List<GBFile> getPathAsList() {
        LinkedList<GBFile> temp = new LinkedList<>();
        if(path != null)
            temp.addAll(path);

        temp.add(this);
        return temp;
    }

    /**
     * Set the path of the file, without updating the fatherID
     * @param pieces New path of the file. This list need to contains this file
     *               as last node
     */
    public void setPathByList(List<GBFile> pieces) {
        this.path = pieces;
        pieces.remove(pieces.size() - 1);
    }

    /**
     * Set the new path of the file without updating the fatherID. This
     * path need to contain this file as last node
     * @param str String that contains the path
     */
    public void setPathByString (String str) {
        this.setPathByString(str, new String());
    }

    /**
     * Set the path from a string. The string prefixToRemove won't be
     * present in the path
     * Example:
     *      str:                "files/new folder"
     *      prefixToRemove:     "files"
     *      path:               []
     *
     * NOTE that the path in this example is empty because the file 'new folder'
     * will be added when any getPath will be called. This because Gson doesn't
     * like an object that contains himself
     *
     * @param str String representation of the path
     * @param prefixToRemove Prefix to remove from the path. The GBFile should have a path
     *                       relative to the root of the storage, not relative to the FS path
     *                       or same randomm folder
     */
    public void setPathByString (String str, String prefixToRemove) {
        // Create a new list that holds the nodes
        path = new LinkedList<>();

        // Divide the path and the prefix in string nodes
        String[] pieces = str.split("/");
        String[] badPieces = prefixToRemove.split("/");

        // skip the intials bad nodes
        int i = 0;
        while(i < badPieces.length && i < pieces.length && pieces[i].equals(badPieces[i]))
            i++;

        // Add all the older except the last
        while(i < pieces.length - 1)
            path.add(new GBFile(pieces[i++], true));
    }

    /**
     * Return the java io.File reference to this file
     * @return Reference to this file
     */
    public File toFile () {
        return toFile(null);
    }

    /**
     * Return the java file object of this file adding the specified prefix to
     * the start of the path (of the file, this prefix doesn't make any difference
     * to the GBFile path)
     * If this file is a wrap of java File or this method was already called the file
     * won't be created, even if the prefix is different
     * @param prefix Prefix to add to the path of the file
     * @return Java file
     */
    public File toFile (String prefix) {
        return (javaFile = javaFile == null ? new File(getPathAsString(prefix)) : javaFile);
    }

    /**
     * Return the java path of this file
     * @return Java Path object of this file
     */
    public Path toPath () {
        return this.toFile().toPath();
    }

    /**
     * Return the java path of this file with the specified prefix
     * @param prefix Prefix to add to the file path
     * @return Java path of this file
     */
    public Path toPath (String prefix) {
        return this.toFile(prefix).toPath();
    }

    /**
     * his method apply the information relative of this file to the file system.
     * If you change the date calling the method 'setCreationDate' the logic representation
     * of this file changes, but the file in the fs not change. to change that information
     * call this method.
     * NOTE: This method will block the thread until the file on the fs is complete updated
     */
    public void applyParams () {
        // TODO: implement this...
    }

    /**
     * Return the children of this file
     * @return
     */
    public List<GBFile> getChildren() {
        return children;
    }

    /**
     * Set the children of this file, removing (logically) the previous
     * @param children New list of children
     */
    public void setChildren(List<GBFile> children) {
        this.children = children;
    }

    /**
     * Return the string representation of this file
     * @return Representation of this file
     */
    @Override
    public String toString() {
        return "GBFile{" +
                "ID=" + ID +
                ", fatherID=" + fatherID +
                ", isDirectory=" + isDirectory +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdateDate=" + lastUpdateDate +
                ", path=" + path +
                ", hash=" + hash +
                ", javaFile=" + javaFile +
                '}';
    }
}