package goboxapi;

/**
 * Created by simonedegiacomi on 10/01/16.
 */
public class GBException {
    /**
     * Message that contains more information about the exception
     */
    private final String message;

    public GBException (String message) {
        super(message);
        this.message = message;
    }

    public String getMessage () {
        return message;
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "message='" + message + '\'' +
                '}';
    }
}
