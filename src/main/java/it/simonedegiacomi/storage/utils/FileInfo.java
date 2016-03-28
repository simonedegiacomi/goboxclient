package it.simonedegiacomi.storage.utils;

import it.simonedegiacomi.goboxapi.GBFile;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by simone on 23/03/16.
 */
public class FileInfo {
    public static void loadFileAttributes (GBFile file) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(file.toFile().toPath(), BasicFileAttributes.class);
        file.setSize(attrs.size());
        file.setCreationDate(attrs.creationTime().toMillis());
        file.setLastUpdateDate(attrs.lastAccessTime().toMillis());
        if(!file.isDirectory())
            file.setMime(new Tika().detect(file.toFile()));
    }
}
