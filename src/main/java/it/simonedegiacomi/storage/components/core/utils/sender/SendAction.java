package it.simonedegiacomi.storage.components.core.utils.sender;

import com.google.common.collect.Range;
import it.simonedegiacomi.goboxapi.GBFile;

/**
 * Created on 27/05/16.
 * @author Degiacomi Simone
 */
public class SendAction {

    private GBFile fileToSend;

    private boolean thumbnail;

    private Range<Long> range;

    public GBFile getFileToSend() {
        return fileToSend;
    }

    public void setFileToSend(GBFile fileToSend) {
        this.fileToSend = fileToSend;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Range<Long> getRange() {
        return range;
    }

    public void setRange(Range<Long> range) {
        this.range = range;
    }
}