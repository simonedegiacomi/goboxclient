package it.simonedegiacomi.storage.Preview;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.Preview.annotations.PreviewerKind;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class ImagePreviewer implements Previewer {

    private static final double RESIZE_RATIO = 0.1;

    private static final String PATH = Config.getInstance().getProperty("path");

    @PreviewerKind(src = "image", dst="image/png")
    @Override
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        // Read the image
        BufferedImage src = ImageIO.read(file.toFile(PATH));

        // Calculate final resolution
        int finalWidth = (int)(src.getWidth() * RESIZE_RATIO);
        int finalHeight = (int)(src.getHeight() * 0.1);

        // Create resized buffered image
        BufferedImage dst = new BufferedImage(finalWidth, finalHeight, src.getType());

        // Get the Graphics object
        Graphics2D g = dst.createGraphics();

        // Draw in the new buffered image the original image resizing it
        g.drawImage(src, 0, 0, finalWidth, finalHeight, null);

        // Render the resized image
        g.dispose();

        // Write the resized image
        ImageIO.write(dst, "png", out);

        // Close the stream
        out.close();
    }
}