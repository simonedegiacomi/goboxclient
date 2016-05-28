package it.simonedegiacomi.storage.components.core.utils.sender.preview;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.components.core.utils.sender.preview.annotations.PreviewerKind;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * This object uses different implementations of the Previewer interface to
 * get the appropriate previewer for each file
 *
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class SimplePreviewer extends Previewer {

    /**
     * Map that contains the know implementations
     */
    private final HashMap<String, Previewer> previewers = new HashMap<>();

    public SimplePreviewer () {
        // TODO: loads all the Previewer using reflection
        Previewer[] availablePreviewers = {
                new ImagePreviewer(),
                new PDFPreviewer(),
                new VideoPreviewer(),
                new AudioPreview()
        };

        // Define the arguments of the method that the class need to implement
        Class[] methodArgs = {GBFile.class, OutputStream.class};

        // Iterate each object to fill the previewers map
        for (Previewer previewer : availablePreviewers) {
            try {
                // Get the method that generate the preview
                Method method = previewer.getClass().getMethod("getPreview", methodArgs);

                // Get the annotation
                PreviewerKind annotation = method.getAnnotation(PreviewerKind.class);
                previewers.put(annotation.src(), previewer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public boolean canHandle (GBFile file) {
        if (getAppropriatePreviewer(file) == null)
            return false;
        return true;
    }

    @Override
    public String getPreviewKind (GBFile file) {
        return getAppropriatePreviewer(file).getPreviewKind(file);
    }

    /**
     * Search in the map the appropriate previewer and generate the preview
     * @param file File of which generate the preview
     * @param out Output stream to which write the preview
     * @throws IOException
     */
    @Override
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        // Get the appropriate previewer
        Previewer previewer = getAppropriatePreviewer(file);
        if(previewer == null)
            return;

        // And generate the preview
        previewer.getPreview(file, out);
    }

    /**
     * Search in the previewers map to find the appropriate previewer.
     * It first try with the exact mime of the file, then try to check if the mime of the file
     * starts with the kind of the previewer.
     * In either case, it also check if the previewer can handle this file, calling the 'canHandle'
     * Method. If no previewers are found, a null reference will be returned.
     * @param file
     * @return
     */
    private Previewer getAppropriatePreviewer (GBFile file) {
        String fileKind = file.getMime();

        // Check if a previewer with the same type of the file exist
        if (previewers.containsKey(fileKind) && previewers.get(fileKind).canHandle(file))
            return previewers.get(fileKind);

        // Check if a previewer with a similar name exist
        for(Map.Entry<String, Previewer> entry : previewers.entrySet()) {
            if (fileKind.startsWith(entry.getKey()) && entry.getValue().canHandle(file))
                return entry.getValue();
        }

        return null;
    }
}
