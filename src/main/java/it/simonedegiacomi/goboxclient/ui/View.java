package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.sync.Sync;
import it.simonedegiacomi.sync.Work;

import java.util.Set;

/**
 * This is the View in the MVP approach. This interface take care of the communication
 * with the user. It shows the data set by the Presenter and catch input from the user
 * and proxy it to the Presenter.
 *
 * Remember that the View should not be able to access the model in the MVP approach.
 *
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public interface View {

    /**
     * Set the presenter for this view
     * @param presenter Presenter that will be used by this view
     */
    void setPresenter (Presenter presenter);

    /**
     * Return the current presenter used by this view
     * @return Previewer
     */
    Presenter getPresenter ();

    /**
     * Set the client environment
     * @param env
     */
    void setEnvironment (GoBoxEnvironment env);

    /**
     * Set the message the is always visible and indicate the state
     * @param message Message
     */
    void setMessage (String message);

    /**
     * Show the specified message to the user
     * @param error Message to show
     */
    void showError (String error);

    /**
     * Update the view from the environment
     */
    void updateViewFromEnvironment();
}