package it.simonedegiacomi.storage;

/**
 * Created by Degiacomi Simone onEvent 31/12/2015.
 */
public class StorageException extends Exception {

    public static final String FILE_NOT_FOUND = "File not found";

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
