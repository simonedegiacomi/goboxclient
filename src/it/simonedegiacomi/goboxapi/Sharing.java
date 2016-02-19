package it.simonedegiacomi.goboxapi;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import sun.security.provider.SHA;

/**
 * Created by simone on 15/02/16.
 */
@DatabaseTable(tableName = "sharing")
public class Sharing {

    @DatabaseField(columnName = "ID", generatedId = true, canBeNull = false)
    private long id;

    @DatabaseField(columnName = "file_ID", canBeNull = false)
    private long fileID;

    private GBFile file;

    public Sharing () {

    }

    public Sharing (GBFile file) {
        this.setFile(file);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GBFile getFile() {
        return file;
    }

    public void setFile(GBFile file) {
        this.file = file;
        this.fileID = file.getID();
    }
}
