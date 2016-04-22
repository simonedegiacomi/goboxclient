package it.simonedegiacomi.storage.utils;

import it.simonedegiacomi.goboxapi.GBFile;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created on 15/04/16.
 * @author Degiacomi Simone
 */
public class MyFileUtils {

    private static String TRASH;

    public static void setTrashPath (String trashPath) {
        MyFileUtils.TRASH = trashPath;
    }

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
     * @param fileToRemove File to remove
     */
    public static void deleteR (File fileToRemove) {
        deleteR(fileToRemove, false);
    }

    /**
     * Delete the specified file. If the file is a directory, also his children will be deleted
     * @param fileToRemove File to remove
     * @param trashed Was the file trashed?
     */
    public static void deleteR (File fileToRemove, boolean trashed) {

        // If it's a directory
        if(fileToRemove.isDirectory()) {

            // First remove all the children
            for (File file : fileToRemove.listFiles())
                deleteR(trashed ? file : new File(TRASH + fileToRemove.toString()));
        }

        // Delete the file
        if (trashed)
            new File(TRASH + fileToRemove.toString()).delete();
        else
            fileToRemove.delete();
    }

    /**
     * Load the file attributes from the disk to the object
     * @param file File to fill with the information
     * @throws IOException
     */
    public static void loadFileAttributes (GBFile file) throws IOException {

        // Get the basic file attributes
        BasicFileAttributes attrs = Files.readAttributes(file.toFile().toPath(), BasicFileAttributes.class);

        // Read the size
        file.setSize(attrs.size());

        // Read the creation and last update date
        file.setCreationDate(attrs.creationTime().toMillis());
        file.setLastUpdateDate(attrs.lastAccessTime().toMillis());

        // If is not a directory
        if(!file.isDirectory()) {

            // Load the mime
            file.setMime(new Tika().detect(file.toFile()));
        }
    }

    /**
     * Hide the specified file
     * @param file File to trash
     */
    public static void trash(File file) throws IOException {

        moveTrash(file, false);
    }

    /**
     * Show the file that was hided
     * @param file File to untrash
     */
    public static void untrash(File file) throws IOException {

        moveTrash(file, true);
    }

    /**
     * Change the visibility of the specified file
     * @param file File to trash/untrash
     * @param toTrash Trashed or not
     * @throws IOException
     */
    public static void moveTrash(File file, boolean toTrash) throws IOException {

        // String path of the file
        String stringPath = file.toString();

        // Create the trashed file
        File trashedFile;

        if (toTrash)
            trashedFile = new File(TRASH + stringPath);
        else
            trashedFile = new File(stringPath.substring(stringPath.indexOf(TRASH)));

        // Move the file
        Files.move(file.toPath(), trashedFile.toPath());
    }
}