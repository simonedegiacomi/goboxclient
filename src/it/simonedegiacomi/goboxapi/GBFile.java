package it.simonedegiacomi.goboxapi;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class used to create the logic database representation
 * of a file.
 *
 * Created by Degiacomi Simone onEvent 24/12/2015.
 */
@DatabaseTable(tableName = "file")
public class GBFile {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(GBFile.class.getName());

    /**
     * The ID of the ROOT directory (well.. file)
     */
    public static final long ROOT_ID = 1;
    public static final long UNKNOWN_ID = -1;
    public static final long UNKNOWN_FATHER = UNKNOWN_ID;
    public static final GBFile ROOT_FILE = new GBFile(ROOT_ID, UNKNOWN_FATHER, "root", true);

    /**
     * Id of the file. Is not final because when the
     * file is created the ID is not known, but we now it
     * only when is inserted onEvent the database
     */
    @DatabaseField(generatedId = true, canBeNull = false)
    private long ID = UNKNOWN_ID;

    /**
     * Id of the father, 0 in case that the file is in
     * the root
     */
    @DatabaseField(canBeNull = false, columnName = "father_ID")
    private long fatherID = UNKNOWN_FATHER;

    /**
     * Indicate if the file is a 'real' file or
     * a directory
     */
    @DatabaseField(canBeNull = false, columnName = "is_directory")
    private boolean isDirectory;

    /**
     * Size of the file in bytes
     */
    @DatabaseField
    private long size;

    /**
     * Name of the file
     */
    @DatabaseField(canBeNull = false)
    private String name;

    /**
     * Date of the creation of this file
     */
    @DatabaseField(columnName = "creation")
    private long creationDate;

    /**
     * Date of the last update of this file
     */
    @DatabaseField(columnName = "last_update")
    private long lastUpdateDate;

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
     * This field shouldn't be serialized, so the 'transient' keyword
     */
    private transient File javaFile;

    @DatabaseField(columnName = "mime", dataType = DataType.STRING)
    private String mime;

    /**
     * List of children of this file
     */
    private List<GBFile> children;

    private GBFile () { }

    public GBFile (long id) {
        this.ID = Math.max(UNKNOWN_ID, id);
    }

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
     * be removed from the path obtained from the java file and will not be included in the path,
     * this because the prefix doesn't make sense in the GoBox Storage. But, because you may need it,
     * when you call the method 'toFile' you'll get the same file you pass now tot he constructor (so
     * with the path prefix)
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
    public void loadAttributes () throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(javaFile.toPath(), BasicFileAttributes.class);
        this.size = attrs.size();
        this.creationDate = attrs.creationTime().toMillis();
        this.lastUpdateDate = attrs.lastAccessTime().toMillis();
        if(!javaFile.isDirectory())
            this.mime = new Tika().detect(javaFile);
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
     * to change also the lastUpdateDate
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
        this.setPathByString(str, null);
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
     *                       or same random folder
     */
    public void setPathByString (String str, String prefixToRemove) {
        // Create a new list that holds the nodes
        path = new LinkedList<>();

        // Divide the path and the prefix in string nodes
        String[] pieces = str.split("/");
        String[] badPieces = prefixToRemove == null ? new String[0] : prefixToRemove.split("/");

        // skip the intials bad nodes
        int i = 0;
        while(i < badPieces.length && i < pieces.length && pieces[i].equals(badPieces[i]))
            i++;

        if(i == pieces.length) {
            // All the path is the file! this means that this is the root!
            this.ID = ROOT_ID;
            return;
        }
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
     * his method apply the information relative of this file to the file system.
     * If you change the date calling the method 'setCreationDate' the logic representation
     * of this file changes, but the file in the fs not change. to change that information
     * call this method.
     * NOTE: This method will block the thread until the file onEvent the fs is complete updated
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

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
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