package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.GoBoxFacade;
import it.simonedegiacomi.sync.Work;

import java.util.HashSet;
import java.util.Set;

/**
 * This is the model of the gobox program. This class s just an alias for the facade class.
 * Created on 21/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxModel implements Model {

    /**
     * Set of listener
     */
    private final Set<Runnable> updateListeners = new HashSet<>();

    /**
     * Flash and error messages
     */
    private String flashMessage, error;

    @Override
    public void setFlashMessage(String message) {
        this.flashMessage = message;
        update();
    }

    @Override
    public String getFlashMessage() {
        return flashMessage;
    }

    @Override
    public void setError(String error) {
        this.error = error;
        update();
    }

    public String getError () {
        return error;
    }

    @Override
    public void addOnUpdateListener(Runnable runnable) {
        updateListeners.add(runnable);
    }

    @Override
    public void clearFlashMessageAndError() {
        flashMessage = error = null;
    }

    private void update () {
        updateListeners.forEach(Runnable::run);
    }

}