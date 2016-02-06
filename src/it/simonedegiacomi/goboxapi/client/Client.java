package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.GBFile;

import java.io.IOException;
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
     * Check if the client is connected to the server.
     * @return Connected to the server or not
     */
    public boolean isOnline ();

    /**
     * Return the list of the file inside an directory
     * @param father Directory to list
     * @return Children of the directory
     * @throws ClientException
     */
    public GBFile[] listDirectory (GBFile father) throws ClientException;

    /**
     * Retrive the file from the storage and save it
     * to the file position saved inside the GBFile
     * passed as argument
     * @param file File to retrieve. The file must have his ID
     *             and a valid path, that will used to save
     *             the received file
     * @throws ClientException Exception thrown in case of
     * invalid id, network error or io error while saving the
     * file to the disk
     */
    public void getFile (GBFile file) throws ClientException, IOException;

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
    public void getFile (GBFile file, OutputStream dst) throws ClientException, IOException;

    /**
     * Create a new directory
     * @param newDir Directory to create
     * @throws ClientException
     */
    public void createDirectory (GBFile newDir) throws ClientException;

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
     * @throws ClientException Exception Network error, null file or invalid
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
    public void updateFile (GBFile file, InputStream stream) throws ClientException;

    /**
     * Update a file in the storage. The same as update,
     * but the stream is obtained from the file
     * @param file File to update
     */
    public void updateFile (GBFile file) throws ClientException;

    /**
     * Set the listener for the SyncEvent received from the storage
     * @param listener Listener that will called with the relative
     *                 event
     */
    public void setSyncEventListener (SyncEventListener listener);

    /**
     * Talk to the storage and tell to it the last ID of the event that
     * this client has heard. The not heard event will come as normal SyncEvent.
     * @param lastHeardId The ID of the last event you received or from you want
     *                    the list
     */
    public void requestEvents (long lastHeardId);
}