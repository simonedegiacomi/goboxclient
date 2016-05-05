package it.simonedegiacomi.storage.sender;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Ubject used by the sender to send files and preview
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public interface SenderDestination {

    /**
     * Return the output stream to which the sender can write
     * @return Output stream, to the destination
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Send the specified header to the destination
     * @param headerName Name of the header
     * @param headerValue Value of the header
     */
    void setHeader(String headerName, String headerValue);

    void sendHeaders (int httpCode) throws IOException;
}
