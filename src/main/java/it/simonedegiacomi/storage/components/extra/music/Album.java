package it.simonedegiacomi.storage.components.extra.music;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

/**
 * Created by simone on 28/05/16.
 */
@DatabaseTable(tableName = "music_library_album")
public class Album {

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Artist artist;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private List<Song> songs;

    @DatabaseField()
    private long releaseDate;

    private String name;

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(long releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
