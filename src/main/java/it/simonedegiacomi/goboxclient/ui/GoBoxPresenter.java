package it.simonedegiacomi.goboxclient.ui;

/**
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxPresenter implements Presenter {

    private View view;

    private Model model;

    @Override
    public void setView(View view) {
        this.view = view;
    }

    @Override
    public View getView() {
        return view;
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
        model.connectClient();
    }

    @Override
    public void disconnectClient() {
        model.shutdownClient();
    }

    @Override
    public void exitProgram() {
        model.shutdownClient();
    }

    @Override
    public void setSync(boolean state) {
        model.setSyncing(state);
    }
}