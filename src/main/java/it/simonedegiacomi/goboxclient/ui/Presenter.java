package it.simonedegiacomi.goboxclient.ui;

import java.util.Set;

/**
 * This is the Presenter Interface in the MVP. This interface says to the view what it needs
 * to show (from the model), and exposes the method that can be called by the view (calling
 * methods of the Model).
 *
 * Created on 4/20/16.
 * @author Degiacomi Simone
 */
public interface Presenter {

    /**
     * Add a new view that the model will use to show the data from the model
     * @param New view to add
     */
    void addView (View view);

    /**
     * Return all the views current used by this presenter.
     * @return Current views used by the presenter
     */
    Set<View> getViews();

    /**
     * Remove a view from the views list
     * @param viewToRemove View to remove
     */
    void removeView (View viewToRemove);

    /**
     * Set the model that will be used by this presenter
     * @param model Model to use
     */
    void setModel (Model model);

    /**
     * Get the current model used by this presenter
     * @return Current used model
     */
    Model getModel ();

    /**
     * This method can be called by the view and proxy the action to the model
     */
    void connectClient ();

    /**
     * This method can be called by the view and proxy the action to the model
     */
    void disconnectClient ();

    /**
     * This method can be called by the view to exit the program (the Model method will be called).
     */
    void exitProgram ();

    /**
     * This method is also called by the view and call the right model method
     * @param state New state of the model
     */
    void setSync (boolean state);

}