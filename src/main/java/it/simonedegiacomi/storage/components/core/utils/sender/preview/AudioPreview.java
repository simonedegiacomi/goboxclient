package it.simonedegiacomi.storage.components.core.utils.sender.preview;

import com.google.common.io.ByteStreams;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.components.core.utils.sender.preview.annotations.PreviewerKind;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This previewer search in a mp3 file for the album art.
 *
 * @author Degiacomi Simone
 * Created on 28/03/16.
 */
public class AudioPreview extends Previewer {

    private static final String DEFAULT_ALBUM_IMAGE = "/default_album_art.png";

//    /**
//     * Check if the file is a valid mp3 and if it contains an album image
//     * @param file File to check
//     * @return True if the file is a valid mp3 and contains the relative album art, false otherwise
//     */
//    @Override
//    public boolean canHandle(GBFile file) {
//        return true;
////        try {
////            Mp3File mp3 = new Mp3File(file.toFile());
////            return mp3.hasId3v2Tag() && mp3.getId3v2Tag().getAlbumImage() != null;
////        } catch (UnsupportedTagException ex) {
////        } catch (InvalidDataException ex) {
////        } catch (IOException ex) { }
////        return false;
//    }

    /**
     * Return the kind of the album image contained in the mp3. If the file is not an mp3 or if it
     * doesn't contains an image, a null string will be returned.
     * @param file Mp3 file which search for the kind of album image
     * @return Kind of album image if contained in the file
     */
    @Override
    public String getPreviewKind(GBFile file) {
        try {
            Mp3File mp3 = new Mp3File(file.toFile());
            if(mp3.hasId3v2Tag()) {
                ID3v2 v2Tags = mp3.getId3v2Tag();
                return v2Tags.getAlbumImageMimeType();
            }
        } catch (UnsupportedTagException ex) {
        } catch (InvalidDataException ex) {
        } catch (IOException ex) { }
        return "image/png";
    }

    /**
     * Search in the mp3 file th album art, and if it's found, the image is written to the output
     * stream.
     * @param file File to which generate the preview
     * @param out Output stream to which write the generated preview
     * @throws IOException
     */
    @Override
    @PreviewerKind(src = "audio/mpeg")
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        try {
            Mp3File mp3 = new Mp3File(file.toFile());
            if (mp3.hasId3v2Tag()) {
                ID3v2 v2Tags = mp3.getId3v2Tag();
                out.write(v2Tags.getAlbumImage());
            }
        } catch (Exception ex) {
            // If somethings bad happen (UnsupportedTagExcpetion, InvalidDataException and so on)
            // send the default image
            ByteStreams.copy(getClass().getResourceAsStream(DEFAULT_ALBUM_IMAGE), out);
        }
    }
}