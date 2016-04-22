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
    public void connectClient() {
        model.connect();
    }

    @Override
    public void exitProgram() {
        model.shutdown();
    }

    @Override
    public void setSync(boolean state) {
        model.setSyncing(state);
    }

    /**
     * Call this method to refresh the view with the data from the model
     */
    public void refreshView () {
        for (View view : views) {
            view.setClientState(model.getClientState());
            view.setMessage(model.getFlashMessage());
            view.setSyncState(model.isSyncing());
            view.setCurrentWorks(model.getCurrentWorks());
        }
    }
}