package mydb;

import java.sql.Date;

/**
 * Class used to create the logic database rappresentation
 * of a file.
 * Created by Degiacomi Simone on 24/12/2015.
 */
public class DBFile {

    /**
     * The ID of the ROOT directory (well.. file)
     */
    public static final long ROOT_ID = 0;

    /**
     * Id of the file. Is not final because when the
     * file is created the ID is not knowed, but we now it
     * only when is insterter on the database
     */
    private long ID;

    /**
     * Id of the father, 0 in case that the file is in
     * the root
     */
    private long fatherID;

    /**
     * Indicate if the file is a 'real' file or
     * a directory
     */
    private final boolean isDirectory;

    /**
     * Indicate if the file exist or if is
     * hidden because ws deleted.
     */
    private boolean visible;

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
    private Date creationDate, lastUpdateDate;

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public DBFile(String name, long fatherID, boolean isDirectory) {
        this.name = name;
        this.fatherID = fatherID;
        this.isDirectory = isDirectory;
        this.creationDate = new Date(0);
        this.lastUpdateDate = new Date(0);
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param ID ID of the file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public DBFile(long ID, long fatherID, String name, boolean isDirectory) {
        this.ID = ID;
        this.fatherID = fatherID;
        this.name = name;
        this.isDirectory = isDirectory;
        this.creationDate = new Date(0);
        this.lastUpdateDate = new Date(0);
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
    protected void setID(long ID) {
        this.ID = ID;
    }

    /**
     * Set the ID of the father.
     * Only the classes inside this package, or that
     * extends this class should edit the ID of the file
     * @param fatherID ID of the father
     */
    protected void setFatherID(long fatherID) {
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

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return "DBFile{" +
                "ID=" + ID +
                ", fatherID=" + fatherID +
                ", isDirectory=" + isDirectory +
                ", visible=" + visible +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", lastUpdateDate='" + lastUpdateDate + '\'' +
                '}';
    }
}
