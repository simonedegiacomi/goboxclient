package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxclient.GoBoxFacade;
import it.simonedegiacomi.sync.Work;

import java.util.List;

/**
 * This is the model of the gobox program. This class s just an alias for the facade class.
 * Created on 21/04/16.
 * @author Degiacomi Simone
 */
public class GoBoxModel implements Model {

    /**
     * Facade object to which delegate the actions
     */
    private final GoBoxFacade facade;

    public GoBoxModel (GoBoxFacade facade) {
        this.facade = facade;
    }

    @Override
    public ClientState getClientState() {
        return facade.getClient().getState;
    }

    @Override
    public void connectClient() {
        facade.connectClient();
    }

    @Override
    public void shutdownClient() {
        facade.shutdown();
    }

    @Override
    public boolean isStorageConnected() {
        return facade.isStorageConnected();
    }

    @Override
    public boolean isSyncing() {
        return facade.isSyncing();
    }

    @Override
    public void setSyncing(boolean sync) {
        facade.setSyncing(sync);
    }

    @Override
    public List<Work> getWorks() {
        return facade.getCurrentWorks();
    }

    @Override
    public boolean isStorageMode() {
        return facade.isStorageMode();
    }

    @Override
    public void exitProgram() {
        facade.shutdown();
    }
}
