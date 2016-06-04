package it.simonedegiacomi.goboxclient.ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxPresenter implements Presenter {

    private final Set<View> views = new HashSet<View>();

    private Model model;

    public GoBoxPresenter(Model model) {
        this.model = model;
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
    public void setModel(Model model) {
        this.model = model;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void exitProgram() {
        model.shutdown();
    }

    /**
     * Call this method to refresh the view with the data from the model
     */
    @Override
    public void updateView () {
        for (View view : views) {
            view.setClient(model.getClient());
            view.setMessage(model.getFlashMessage());
            view.setCurrentWorks(model.getCurrentWorks());
            if(model.getError() != null)
                view.showError(model.getError());
        }
    }

    @Override
    public boolean isStorage() {
        return true;
    }
}