package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.sync.Work;

import java.util.Set;

/**
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class CLIView implements View {

    private Presenter presenter;

    public CLIView (Presenter presenter) {
        this.presenter = presenter,
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setClientState(ClientState state) {

    }

    @Override
    public void setSyncState(boolean enabled) {

    }

    @Override
    public void setWorks(Set<Work> worksQueue) {

    }
}