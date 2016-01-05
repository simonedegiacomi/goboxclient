package goboxapi.authentication;

/**
 * Created by Degiacomi Simone on 31/12/15.
 */
public class AuthException extends Exception {
    private String message;

    public AuthException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "AuthException{" +
                "message='" + message + '\'' +
                '}';
    }
}
