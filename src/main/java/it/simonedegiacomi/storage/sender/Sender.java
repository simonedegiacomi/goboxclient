package it.simonedegiacomi.storage.sender;

import com.google.common.collect.Range;
import com.google.common.io.ByteStreams;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.sender.preview.CachedPreviewer;
import it.simonedegiacomi.storage.sender.preview.Previewer;
import it.simonedegiacomi.storage.utils.MyZip;
import org.h2.store.LimitInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.security.InvalidParameterException;

/**
 * This class sends files and preview, writing the content to the http
 * response body.
 *
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
public class Sender {

    /**
     * Previewer used to send the preview
     */
    private Previewer previewer = new CachedPreviewer();

    /**
     * Send the zipped version of a directory.
     * NOTE that this method doesn't close the connection stream
     * @param file Directory to send
     * @param dst Connection which body will be filled with the zipped folder
     * @throws IOException Exception while zipping the folder or writing to the
     *  connection ouput stream
     */
    public void sendDirectory (GBFile file, SenderDestination dst) throws IOException {

        // Set the type of response
        dst.setHeader("Content-Type", "application/zip");

        // Get the output stream
        OutputStream rawStreamToServer = dst.getOutputStream();

        /// Zip and send the folder
        MyZip.zipFolder(file.toFile(), rawStreamToServer);
    }

    /**
     *
     * NOTE that this method doesn't close the connection stream
     * @param file
     * @param dst
     * @throws IOException
     */
    public void sendPreview (GBFile file, SenderDestination dst) throws IOException {

        // Check if the previewer can handle this file
        if(!previewer.canHandle(file))
            return;

        // Specify the type of previewe
        dst.setHeader("Content-Type", previewer.getPreviewKind(file));

        // Get the connection output stream
        OutputStream out = dst.getOutputStream();

        // Write into the stream the preview
        previewer.getPreview(file, out);
    }

    /**
     * Send the specified file.
     * This method is just an alias of {@link #sendFile(GBFile, HttpURLConnection, Range) sendFile}.
     * NOTE that this method doesn't close the connection stream
     * @param file File to send
     * @param dst Connection of which body will be filled with the file
     * @throws IOException Exception while sending the file
     */
    public void sendFile (GBFile file, SenderDestination dst) throws IOException {

        // Wrap the send file method
        sendFile(file, dst, null);
    }

    /**
     * Send the specified file. If the range is not a null value, only the specified range of file
     * will be sent.
     * NOTE that this method doesn't close the connection stream
     * @param gbFile File to send
     * @param dst Connection to send
     * @param range Range of bytes of the file
     * @throws IOException Exception while sending the file
     */
    public void sendFile (GBFile gbFile, SenderDestination dst, Range<Long> range) throws IOException {
        if (gbFile == null || !gbFile.toFile().exists())
            throw new InvalidParameterException("file not valid");

        // Get the java file
        File file = gbFile.toFile();

        // Check if the file is a directory
        if (file.isDirectory())
            throw new InvalidParameterException("This method cannot send a directory");

        // If the file know his length, specify it
        if(gbFile.getSize() > 0)
            dst.setHeader("Content-Length", String.valueOf(gbFile.getSize()));

        // If the file knows his mime, specify it
        if(gbFile.getMime() !=  null)
            dst.setHeader("Content-Type", gbFile.getMime());

        // Get the output stream to the server
        OutputStream rawStreamToServer = dst.getOutputStream();

        // Open the file
        InputStream fromFile = new FileInputStream(file);

        // If the range is specified
        if (range != null) {

            // Create a new limited input stream
            LimitInputStream limitInputStream = new LimitInputStream(fromFile, range.upperEndpoint());

            // Skip the first byte
            limitInputStream.skip(range.lowerEndpoint());

            fromFile = limitInputStream;
        }

        // Send the file
        ByteStreams.copy(fromFile, rawStreamToServer);

        // Close the file stream
        fromFile.close();
    }
}