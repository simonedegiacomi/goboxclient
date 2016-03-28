package it.simonedegiacomi.utils;

/**
 * This class is used to communicate message from a speaker to a listener.
 * When the speaker say something calling the 'say' method, the callback
 * of the listener implementation will called.
 *
 * Created on 28/02/16.
 * @author Degiacomi Simone
 */
public class Speaker {

    /**
     * Interface to implement by the listener
     */
    public interface Listener {
        public void onMessage (String message);
    }

    /**
     * Listener of this speaker
     */
    private final Listener listener;

    /**
     * Create a new Speaker object for the listener passed as argument
     * @param listener Listener of the new speaker
     */
    public Speaker (Listener listener) {
        this.listener = listener;
    }

    /**
     * Say something. This method call the 'onMessage' method of the listener
     * @param what Message to communicate
     */
    public void say (String what) {
        listener.onMessage(what);
    }
}