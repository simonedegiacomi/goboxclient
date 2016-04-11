package it.simonedegiacomi.storage.sender.preview;

import com.google.common.io.Files;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import org.apache.tika.Tika;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Phaser;

/**
 * This object wrap the SimplePreview object, providing a cache system
 *
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class CachedPreviewer extends Previewer {

    /**
     * Path of the folder dedicated to the cache of the previews
     */
    private final String CACHE_PATH;

    /**
     * Previewer used to generate the previews
     */
    private final SimplePreviewer previewer = new SimplePreviewer();

    /**
     * Used to read the kind of cached preview
     */
    private final Tika tika = new Tika();

    /**
     * Object used to synchronize the method eraseAll
     * TODO: evaluate if is needed
     */
    private final Phaser operations = new Phaser();

    /**
     * Create a new CachedPreview using the folder specified in the property
     * 'cachePath' in the configuration
     */
    public  CachedPreviewer () {
        this(Config.getInstance().getProperty("cachePath"));
    }

    /**
     * Create a new CachedPreviewer that use the folder specified as argument
     * @param path Path where the preview will be cached
     */
    public CachedPreviewer (String path) {
        this.CACHE_PATH = path;
    }

    /**
     * Check if a cached version of the preview of the file exist
     * @param file File to check
     * @return Existence of the cached preview
     */
    private boolean isCached (GBFile file) {
        return new File(CACHE_PATH + file.getID()).exists();
    }

    @Override
    public boolean canHandle(GBFile file) {
        return previewer.canHandle(file);
    }

    @Override
    public String getPreviewKind(GBFile file) {
        if(isCached(file)) {
            try {
                return tika.detect(new File(CACHE_PATH + file.getID()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return previewer.getPreviewKind(file);
    }

    /**
     * Check if the preview of this file was already created and cached. If it is
     * the cached version is written to the output stream, otherwise a new preview
     * will be created, using the SimplePreviewer.
     *
     * NOTE that this method doesn't close the Output Stream
     * @param file File to which get the preview
     * @param out Output stream to write the preview
     * @throws IOException
     */
    @Override
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        // Check if a cached preview exists for this file
        if(!isCached(file)) {
            // Otherwise generate and cache it
            cache(file);
        }

        // Read the cached preview
        getCache(file, out);
    }

    /**
     * Cache the file passed as argument. This method override any previous cached preview
     * of this file
     * @param fileToCache File which preview is to cache
     * @throws IOException
     */
    public void cache (GBFile fileToCache) throws IOException {
        operations.register();

        // Create the new file
        FileOutputStream out = new FileOutputStream(new File(CACHE_PATH + fileToCache.getID()));

        // Generate the preview
        previewer.getPreview(fileToCache, out);

        // Flush the file
        out.close();

        operations.arrive();
    }

    /**
     * Write to the output stream the cached preview of the specified file
     * @param file File of the requested preview
     * @param out Output stream to write the cached preview
     * @throws IOException
     */
    private void getCache (GBFile file, OutputStream out) throws IOException {
        operations.register();

        File preview = new File(CACHE_PATH + file.getID());
        Files.copy(preview, out);

        operations.arrive();
    }

    /**
     * Delete the cached preview of the specified file
     * @param file File which cache need to be invalidate
     * @throws IOException
     */
    private void invalidate (GBFile file) throws IOException {
        if(isCached(file))
            new File(CACHE_PATH + file.getID()).delete();
    }

    /**
     * Delete all the cached previews
     * @throws IOException
     */
    public void eraseAllCache () throws IOException {
        operations.awaitAdvance(operations.getPhase());

        // Iterate and delete all the cached previews
        for(File preview : new File(CACHE_PATH).listFiles())
            preview.delete();
    }
}
