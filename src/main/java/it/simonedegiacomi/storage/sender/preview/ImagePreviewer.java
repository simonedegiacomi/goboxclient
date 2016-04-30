package it.simonedegiacomi.storage.sender.preview;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.sender.preview.annotations.PreviewerKind;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class ImagePreviewer extends Previewer {

    /**
     * Default kind of image previews
     */
    private static final String DEFAULT_PREVIEW_KIND = "png";

    private static final int PREVIEW_WIDTH = 256;

    @Override
    public String getPreviewKind(GBFile file) {
        return DEFAULT_PREVIEW_KIND;
    }

    @PreviewerKind(src = "image")
    @Override
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        // Read the image
        BufferedImage src = ImageIO.read(file.toFile());

        // Calculate final resolution
        int finalWidth = PREVIEW_WIDTH;
        int finalHeight = (int) (((double) src.getHeight()) / ((double)src.getWidth()) * PREVIEW_WIDTH);

        // Resize
        BufferedImage dst = resize(src, finalWidth, finalHeight);

        // Write the resized image
        ImageIO.write(dst, DEFAULT_PREVIEW_KIND, out);

        // Close the stream
        out.close();
    }

    public static BufferedImage resize (BufferedImage src, int finalWidth, int finalHeight) {
        // Create resized buffered image
        BufferedImage dst = new BufferedImage(finalWidth, finalHeight, src.getType());

        // Get the Graphics object
        Graphics2D g = dst.createGraphics();

        // Draw in the new buffered image the original image resizing it
        g.drawImage(src, 0, 0, finalWidth, finalHeight, null);

        // Render the resized image
        g.dispose();

        return dst;
    }
}