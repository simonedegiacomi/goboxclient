package it.simonedegiacomi.storage.sender.preview;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.sender.preview.annotations.PreviewerKind;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Create the preview of a pdf document rendering the first page as an image
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class PDFPreviewer extends Previewer {

    private static final String DEFAULT_PREVIEW_KIND = "png";

    @Override
    public String getPreviewKind(GBFile file) {
        return DEFAULT_PREVIEW_KIND;
    }

    @Override
    @PreviewerKind(src = "application/pdf")
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        // Load the document
        PDDocument pdf = PDDocument.load(file.toFile());

        // Create the pdf render
        PDFRenderer renderer = new PDFRenderer(pdf);

        // Render the first page
        BufferedImage renderedPage = renderer.renderImage(0);

        // Write the image
        ImageIO.write(renderedPage, "png", out);

        // Close the pdf
        pdf.close();
    }
}