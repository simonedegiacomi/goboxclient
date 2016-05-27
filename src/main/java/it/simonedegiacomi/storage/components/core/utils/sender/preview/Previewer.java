package it.simonedegiacomi.storage.components.core.utils.sender.preview;

import it.simonedegiacomi.goboxapi.GBFile;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public abstract class Previewer {

    /**
     * This method should return an boolean that represents the ability to generate
     * the preview for the kind of the specified file
     * @param file
     * @return ability to generate the associated preview
     */
    public boolean canHandle (GBFile file) {
        return true;
    }

    /**
     * This method should return the kind of the preview that will generate if the method
     * 'getPreview' is called with the specified file
     * @param file
     * @return Kind of preview that will be generated if the method getPreview is called
     */
    public abstract String getPreviewKind (GBFile file);

    /**
     * This method should generate the preview and write in the specified output stream
     * @param file File to which generate the preview
     * @param out Output stream to which write the generated preview
     * @throws IOException
     */
    public abstract void getPreview (GBFile file, OutputStream out) throws IOException;
}