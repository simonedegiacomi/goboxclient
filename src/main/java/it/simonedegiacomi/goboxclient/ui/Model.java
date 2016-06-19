package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.GoBoxFacade;
import it.simonedegiacomi.sync.Work;

import java.util.Set;

/**
 * This is the Interface Model in the MVP.
 * The model is an interface that defines the data to display and the action that the user
 * can perform. Actually This class take care of the data to display in the view (by the Presenter)
 * and exposes the methods that can be called by the view (always trough the Presenter).
 *
 * Created on 4/20/16.
 * @author Degiacomi Simone
 */
public interface Model {

    /**
     * Set the message of the current action
     * @param s Message of current action
     */
    void setFlashMessage(String s);

    String getFlashMessage();

    void setError (String error);

    String getError();

    /**
     * Add a new listener that will be called when the modle updates
     * @param runnable Runnable to execute
     */
    void addOnUpdateListener (Runnable runnable);

    /**
     * Thi method clear the flash message and error
     */
    void clearFlashMessageAndError();
}