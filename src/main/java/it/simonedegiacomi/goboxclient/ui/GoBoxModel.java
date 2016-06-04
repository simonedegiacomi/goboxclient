package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
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


    private String flashMessage, error;

    public GoBoxModel (GoBoxFacade facade) {
        this.facade = facade;
    }

    @Override
    public GBClient getClient() {
        return facade.getClient();
    }

    @Override
    public void shutdown() {
        facade.shutdown();
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
        this.error = error;
    }

    public String getError () {
        return error;
    }
}