package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxclient.GoBoxEnvironment;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxPresenter implements Presenter {

    private final Set<View> views = new HashSet<View>();

    private final GoBoxEnvironment env;

    private final Model model;

    public GoBoxPresenter(GoBoxEnvironment env) {
        this.env = env;
        this.model = env.getModel();
    }

    @Override
    public void addView(View view) {
        views.add(view);
    }

    @Override
    public Set<View> getViews() {
        return views;
    }

    @Override
    public void removeView(View viewToRemove) {
        views.remove(viewToRemove);
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void exitProgram() {
        env.shutdown();
        System.exit(0);
    }

    /**
     * Call this method to refresh the view with the data from the model
     */
    @Override
    public void updateView () {
        for (View view : views) {

            // Update view
            view.updateViewFromEnvironment();

            // Show messages
            if (model.getFlashMessage() != null) {
                view.setMessage(model.getFlashMessage());
            }

            // Show errors
            if(model.getError() != null) {
                view.showError(model.getError());
            }
        }

        // clear messages
        model.clearFlashMessageAndError();
    }
}