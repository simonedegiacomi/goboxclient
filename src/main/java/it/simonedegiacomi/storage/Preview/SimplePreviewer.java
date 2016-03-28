package it.simonedegiacomi.storage.Preview;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.Preview.annotations.PreviewerKind;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class SimplePreviewer implements Previewer {

    private final HashMap<String, Previewer> previewers = new HashMap<>();

    public SimplePreviewer () {
        Previewer[] availablePreviewers = {
                new ImagePreviewer(),
                new PDFPreviewer()
        };
        Class[] methodArgs = {GBFile.class, OutputStream.class};
        for (Previewer previewer : availablePreviewers) {
            try {
                Method method = previewer.getClass().getMethod("getPreview", methodArgs);
                PreviewerKind annotation = method.getAnnotation(PreviewerKind.class);
                previewers.put(annotation.src(), previewer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        Previewer previewer = getAppropriatePreviewer(file);
        if(previewer == null)
            return;
        previewer.getPreview(file, out);
    }

    private Previewer getAppropriatePreviewer (GBFile file) {

        String fileKind = file.getMime();

        // Check if a previewer with the same type of the file exist
        if (previewers.containsKey(fileKind))
            return previewers.get(fileKind);
    }
}
