package it.simonedegiacomi.storage.Preview;

import it.simonedegiacomi.goboxapi.GBFile;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by simone on 27/03/16.
 */
public interface Previewer {
    public void getPreview (GBFile file, OutputStream out) throws IOException;
}
