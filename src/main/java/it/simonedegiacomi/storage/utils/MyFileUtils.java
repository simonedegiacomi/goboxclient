package it.simonedegiacomi.storage.utils;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;

/**
 * Created on 15/04/16.
 * @author Degiacomi Simone
 */
public class MyFileUtils {

    /**
     * Path of the trash folder
     */
    private final static String TRASH = Config.getInstance().getProperty("trash");

    /**
     * Copy a file from the source to the destination. If the file is a directory,
     * a new folder in the destination will be created end filled with the children
     * @param src Source file
     * @param dst Destination File
     * @throws IOException Errors during the copy
     */
    public static void copyR (File src, File dst) throws IOException {

        // Call the standard method
        Files.copy(src.toPath(), dst.toPath());

        // If it's a directory
        if (src.isDirectory()) {

            // Call the copy method for each children
            for(File file : src.listFiles())
                copyR(file, new File(dst.getPath() + '/' + file.getName()));
        }
    }

    /**
     * Delete the specified file. If the file is a directory, also his children will be deleted
     * @param file File to remove
     */
    public static void delete (File file) {
        if (file == null || !file.exists())
            throw new InvalidParameterException("file not valid");

        // Remove the child first
        if (file.isDirectory()) {
            for(File child : file.listFiles()) {
                delete(child);
            }
        }

        // Finally the file
        file.delete();
    }

    /**
     * Check if the file is trashed. Then call the {@link #deleteR(File)} method
     * @param fileToRemove File to remove
     * @param trashed Was the file trashed?
     */
    public static void delete (GBFile fileToRemove){
        // Change the prefix, so i can move the file easily
        // TODO: Clean approach, this is not thread safe and it's not a good idea
        String temp = null;
        if (fileToRemove.isTrashed()) {
            temp = fileToRemove.getPrefix();
            fileToRemove.setPrefix(TRASH);
        }

        delete(fileToRemove.toFile());

        if (temp != null) {
            fileToRemove.setPrefix(temp);
        }
    }

    /**
     * Load the file attributes from the disk to the object
     * @param file File to fill with the information
     * @throws IOException
     */
    public static void loadFileAttributes (GBFile file) throws IOException {
        if(file == null || !file.toFile().exists())
            throw new InvalidParameterException("file not valid");

        // Get the basic file attributes
        BasicFileAttributes attrs = Files.readAttributes(file.toFile().toPath(), BasicFileAttributes.class);
        file.setSize(attrs.size());
        file.setCreationDate(attrs.creationTime().toMillis());
        file.setLastUpdateDate(attrs.lastAccessTime().toMillis());

        // If is not a directory load the mime
        if(!file.isDirectory()) {
            file.setMime(new Tika().detect(file.toFile()));
        }
    }

    /**
     * Change the visibility of the specified file
     * @param file File to trash/untrash
     * @param toTrash Trashed or not
     * @throws IOException
     */
    public static void moveTrash(GBFile file) throws IOException {
        if (file == null)
            throw new InvalidParameterException("file not valid");

        // TODO: clean

        File trashFile = file.toFile();
        String oldPrefix = file.getPrefix();
        file.setPrefix(TRASH);
        if (file.isTrashed()) {
            trashFile = file.toFile();
            file.setPrefix(oldPrefix);
        }

        // Move the file
        Files.move(file.toFile().toPath(), trashFile.toPath());

        if (file.isTrashed()) {
            file.setPrefix(oldPrefix);
        }
    }

    private static void prepareDestination (File file, File dst) {
        String[] filePieces = file.toString().split("/");
        String[] dstPieces = file.toString().split("/");
        //for (int i = dstPieces.length - 1;i < filePieces; )
    }
}