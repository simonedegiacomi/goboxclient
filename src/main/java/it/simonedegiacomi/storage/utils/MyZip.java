package it.simonedegiacomi.storage.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by simone on 15/02/16.
 */
public class MyZip {

    public static void zipFolder (File folderToZip, OutputStream rawOutput) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(rawOutput);

        addR(zip, "", folderToZip);

        zip.close();
    }

    private static void addR (ZipOutputStream zip, String father, File file) throws IOException {
        if(file.isFile()) {
            zip.putNextEntry(new ZipEntry(father + '/' + file.getName()));
            FileInputStream fileStream = new FileInputStream(file);
            byte buffer[] = new byte[1024];
            int read = 0;
            while((read = fileStream.read(buffer)) > 0)
                zip.write(buffer, 0, read);
            zip.closeEntry();
            zip.flush();
            return;
        }
        for(File child : file.listFiles())
            addR(zip, father + '/' + file.getName(), child);
    }

}
