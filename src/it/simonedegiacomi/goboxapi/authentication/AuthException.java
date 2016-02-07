package it.simonedegiacomi.goboxapi.authentication;

/**
 * Exception of GoBox Auth
 *
 * Created by Degiacomi Simone onEvent 31/12/15.
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
