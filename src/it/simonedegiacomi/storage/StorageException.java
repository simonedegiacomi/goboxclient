package it.simonedegiacomi.storage;

/**
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class StorageException extends Exception {
    private final String message;

    public StorageException(String message) {
        super (message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
