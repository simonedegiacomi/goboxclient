package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxclient.GoBoxFacade;
import it.simonedegiacomi.sync.Work;

import java.util.Set;

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

    private String flashMessage;

    public GoBoxModel (GoBoxFacade facade) {
        this.facade = facade;
    }

    @Override
    public Client.ClientState getClientState() {
        return facade.getClient().getState();
    }

    @Override
    public void connect() {
        facade.connect();
    }

    @Override
    public void shutdown() {
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
    public Set<Work> getCurrentWorks() {
        return facade.getRunningWorks();
    }

    @Override
    public boolean isStorageMode() {
        return facade.isStorageMode();
    }

    @Override
    public void setFlashMessage(String message) {
        this.flashMessage = message;
    }

    @Override
    public String getFlashMessage() {
        return flashMessage;
    }

    @Override
    public void setError(String error) {

    }
}