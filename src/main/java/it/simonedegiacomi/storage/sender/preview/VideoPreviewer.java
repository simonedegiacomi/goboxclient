package it.simonedegiacomi.storage.sender.preview;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.storage.sender.preview.annotations.PreviewerKind;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Degiacomi Simone
 * Created on 27/03/16.
 */
public class VideoPreviewer extends Previewer {

    private static final int FRAME_LIMIT = 100;

    private static final int FRAME_SKIP = 5;

    private static final int GIF_DELAY = 1000;

    private static final double RESIZE_RATIO = 0.3;

    @Override
    public String getPreviewKind(GBFile file) {
        return "image/gif";
    }

    @Override
    @PreviewerKind(src = "video")
    public void getPreview(GBFile file, OutputStream out) throws IOException {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.toFile());
        try {
            grabber.start();
        } catch (FrameGrabber.Exception ex) {
            return;
        }
        int frames = grabber.getLengthInFrames();
        int deltaFrame = (int) (frames / grabber.getFrameRate()) * FRAME_SKIP;
        Java2DFrameConverter converter = new Java2DFrameConverter();

        AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
        gifEncoder.setDelay(GIF_DELAY);
        gifEncoder.setRepeat(0);
        int width = (int) (grabber.getImageWidth() * RESIZE_RATIO);
        int height = (int) (grabber.getImageHeight() * RESIZE_RATIO);
        gifEncoder.setSize(width, height);
        gifEncoder.start(out);

        try {
            int frameCount = 1;
            for (int i = 0;i < FRAME_LIMIT && frameCount < frames; i++) {
                grabber.setFrameNumber(frameCount);
                BufferedImage frame = converter.convert(grabber.grab());
                gifEncoder.addFrame(ImagePreviewer.resize(frame, width, height));
                frameCount += deltaFrame;
            }
            grabber.stop();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }
        gifEncoder.finish();
    }
}
