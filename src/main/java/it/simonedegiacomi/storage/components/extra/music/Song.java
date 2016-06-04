package it.simonedegiacomi.storage.components.extra.music;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import it.simonedegiacomi.goboxapi.GBFile;

/**
 * Created by simone on 28/05/16.
 */
@DatabaseTable(tableName = "music_library_song")
public class Song {

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Artist artist;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Album album;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private GBFile file;

    @DatabaseField
    private String title;

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public GBFile getFile() {
        return file;
    }

    public void setFile(GBFile file) {
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}