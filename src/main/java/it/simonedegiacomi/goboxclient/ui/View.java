package it.simonedegiacomi.goboxclient.ui;

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
     * Set the state of the client.
     *
     * @param state Current state of the client
     */
    void setClientState (ClientState state);

    /**
     * Set if the sync is running
     * @param enabled Is the sync running?
     */
    void setSyncState (boolean enabled);

    /**
     * Set the current works
     * @param worksQueue Running works
     */
    void setWorks (Set<Work> worksQueue);

}