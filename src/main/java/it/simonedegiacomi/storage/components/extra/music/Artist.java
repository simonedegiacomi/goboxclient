package it.simonedegiacomi.storage.components.extra.music;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

/**
 * Created by simone on 28/05/16.
 */
@DatabaseTable(tableName = "music_library_artist")
public class Artist {

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private String name;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private List<Album> albums;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }
}
