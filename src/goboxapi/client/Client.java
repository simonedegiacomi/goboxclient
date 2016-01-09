package goboxapi.client;

import goboxapi.GBFile;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is the interface of the goboxclient api
 * and contains the basic operation that a client
 * can do.
 *
 * Created by Degiacomi Simone on 02/01/2016.
 */
public interface Client {

    /**
     * Get a file from the storage.
     * @param file File to retrive
     * file from the storage
     */
    public void getFile (GBFile file);

    public void getFile (GBFile file, OutputStream dst);

    /**
     * Send a file to the storage.
     * @param file File to send
     * @param stream Stream of the file
     */
    public void uploadFile (GBFile file, Inputstream stream);

    /**
     * Send a file to the storage. The input stream
     * of the file is obtaned from the file
     * @param file File to send
     */
    public void uploadFile (GBFile file);

    /**
     * Remove a file from the sotrage
     * @param file File to remove
     */
    public void removeFile (GBFile file);

    /**
     * Update a file in the storage.
     * PS: If the informations of the file are changed
     * update only that informations, otherwise resend
     * the file
     * @param file File to update
     */
    public void updateFile (GBFile file, InputStream file);

    /**
     * Update a file in the storage. The same ad update,
     * but the stream is obtained from the file
     * @param file File to update
     */
    public void updateFile (GBFile file);

    /**
     * Set the listener for the SyncEvent received from the
     * storage
     * @param listener Listener that will called with the relative
     *                 event
     */
    public void setSyncEventListener (SyncEventListener listener);
}
