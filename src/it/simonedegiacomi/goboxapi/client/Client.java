package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.GBFile;

import java.io.*;

/**
 * This is the interface of the goboxclient api and define the basic operation
 * that a client can do.
 *
 * @author Degiacomi Simone
 * Created on 02/01/2016.
 */
public abstract class Client {

    /**
     * Check if the client is connected to the storage. It doesn't mean
     * that it's using network, just that the client can talk with the storage
     * @return Connected to the server or not
     */
    public abstract boolean isOnline ();

    /**
     * Return a new GBFile with the info retrieved from the storage. If the file is not found
     * a null pointer will be returned. The exception is thrown if there's some connection error.
     *
     * When the file is missing i return a null pointer instead throwing a new exception because
     * you can call this method to check if a file exist, so a null pointer can be a logical value
     * (exist or not).
     * @param file File to look at
     * @return GBFile with the storage information
     * @throws ClientException Exception if there some problem with the communication to
     * the storage
     */
    public abstract GBFile getInfo(GBFile file) throws ClientException;

    /**
     * Retrieve the file from the storage and save it to the file position saved inside the GBFile
     * passed as argument. If the file doesn't exist a new exception is thrown. Int his case an exception
     * because you're supposing that the file exist
     * @param file File to retrieve.
     * @throws ClientException Exception thrown in case of
     * invalid id, network error or io error while saving the
     * file to the disk
     */
    public void getFile (GBFile file) throws ClientException, IOException {
        getFile(file, new FileOutputStream(file.toFile()));
    }

    /**
     * Same as getFile(GBFile) but let you specify the output stream that will be used to write the incoming file
     * from the storage
     * @param file File to download
     * @param dst Destination of the input stream of the file
     * @throws ClientException Exception thrown in case of
     * invalid id or network error
     */
    public abstract void getFile (GBFile file, OutputStream dst) throws ClientException, IOException;

    /**
     * Create a new directory
     * @param newDir Directory to create
     * @throws ClientException thrown if a folder with the same name exist or other reason of the storage
     */
    public abstract void createDirectory (GBFile newDir) throws ClientException;

    /**
     * Send a file to the storage.
     * @param file File to send File to send. The object must have or the field father id or the path.
     * @param stream Stream of the file Stream that will be sent to the storage
     * @throws ClientException Exception Network error or invalid father reference
     */
    public abstract void uploadFile (GBFile file, InputStream stream) throws ClientException, IOException;

    /**
     * Same ad uploadFile(GBFile, InputStream) but this read the file from the path of the GBFile
     * @param file File to send
     * @throws ClientException Exception Network error, null file or invalid
     * father reference
     */
    public void uploadFile (GBFile file) throws ClientException, IOException {
        uploadFile(file, new FileInputStream(file.toFile()));
    }

    /**
     * Remove a file from the storage
     * @param file File to remove
     * @throws ClientException Exception thrown if the id is not valid
     */
    public abstract void removeFile (GBFile file) throws ClientException;

    /**
     * Update a file in the storage.
     * PS: If the information of the file are changed
     * update only that information, otherwise resend
     * the file
     * @param file File to update
     */
    public abstract void updateFile (GBFile file, InputStream stream) throws ClientException;

    /**
     * Update a file in the storage. The same as update,
     * but the stream is obtained from the file
     * @param file File to update
     */
    public abstract void updateFile (GBFile file) throws ClientException;

    /**
     * Set the listener for the SyncEvent received from the storage
     * @param listener Listener that will called with the relative
     *                 event
     */
    public abstract void setSyncEventListener (SyncEventListener listener);

    /**
     * Talk to the storage and tell to it the last ID of the event that
     * this client has heard. The not heard event will come as normal SyncEvent.
     * @param lastHeardId The ID of the last event you received or from you want
     *                    the list
     */
    public abstract void requestEvents (long lastHeardId);

    /**
     * Close the connection with the storage and release all the resources.
     */
    public abstract void shutdown ();
}