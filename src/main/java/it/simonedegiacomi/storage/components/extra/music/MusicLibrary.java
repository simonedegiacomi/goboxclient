package it.simonedegiacomi.storage.components.extra.music;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.components.AttachFailException;
import it.simonedegiacomi.storage.components.ComponentConfig;
import it.simonedegiacomi.storage.components.GBModule;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by simone on 28/05/16.
 */
public class MusicLibrary implements GBModule {

    private final Logger log = Logger.getLogger(MusicLibrary.class);

    private Dao<Artist, Long> artistTable;

    private Dao<Album, Long> albumTable;

    private Dao<Song, Long> songTable;

    @Override
    public void onAttach(StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException {
        try {
            artistTable = DaoManager.createDao(env.getDbConnection(), Artist.class);
            albumTable = DaoManager.createDao(env.getDbConnection(), Album.class);
            songTable = DaoManager.createDao(env.getDbConnection(), Song.class);
        } catch (SQLException ex) {
            throw new AttachFailException("Unable to create dao");
        }
        env.getEmitter().addInternalListener(event -> {
//            if (!event.equals(SyncEvent.EventKind.FILE_CREATED) || !isAudioFile(event.getRelativeFile())) {
//                log.debug("Ignore event");
//                return;
//            }

            log.info("New audio file");
            addAudioFile(event.getRelativeFile());
        });
    }

    @Override
    public void onDetach() {

    }

//    private boolean isAudioFile (GBFile file) {
//
//    }

    private void addAudioFile(GBFile file) {
        try {
            Mp3File mp3File = new Mp3File(file.toFile());
            
        } catch (InvalidDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedTagException e) {
            e.printStackTrace();
        }
    }
}
