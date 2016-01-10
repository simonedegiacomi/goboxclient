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
     * Retrive the file from the storage and save it
     * to the file position saved inside the GBFile
     * passed as argument
     * @param file File to retrive. The file must have his ID
     *             and a valid path, that will used to save
     *             the received file
     * @throws ClientException Exception throwed in case of
     * invalid id, network error or io error while saving the
     * file to the disk
     */
    public void getFile (GBFile file) throws ClientException;

    /**
     * Same as getFile(GBFile) but let you specify the output
     * stream that will be used to write the incoming file
     * from the storage
     * @param file File to download, (Only the id of this object
     *             will be sued)
     * @param dst Destination of the input stream of the file
     * @throws ClientException Exception thrown in case of
     * invalid id or network error
     */
    public void getFile (GBFile file, OutputStream dst) throws ClientException;

    /**
     * Send a file to the storage.
     * @param file File to send File to send. The object must have or the
     *             field father id or the path.
     * @param stream Stream of the file Stream that will be sent to the storage
     * @throws ClientException Exception Network error or invalid father reference
     */
    public void uploadFile (GBFile file, InputStream stream) throws ClientException;

    /**
     * Same ad uploadFile(GBFile, InputStream) but this read the file from
     * the path of the GBFile
     * @param file File to send
     * @throws ClientException Exception Netowork error, null file or invalid
     * father reference
     */
    public void uploadFile (GBFile file) throws ClientException;

    /**
     * Remove a file from the storage
     * @param file File to remove
     * @throws ClientException Exception thrown if the id is not valid
     */
    public void removeFile (GBFile file) throws ClientException;

    /**
     * Update a file in the storage.
     * PS: If the information of the file are changed
     * update only that information, otherwise resend
     * the file
     * @param file File to update
     */
    public void updateFile (GBFile file, InputStream file) throws ClientException;

    /**
     * Update a file in the storage. The same as update,
     * but the stream is obtained from the file
     * @param file File to update
     */
    public void updateFile (GBFile file) throws ClientException;

    /**
     * Set the listener for the SyncEvent received from the
     * storage
     * @param listener Listener that will called with the relative
     *                 event
     */
    public void setSyncEventListener (SyncEventListener listener);
}