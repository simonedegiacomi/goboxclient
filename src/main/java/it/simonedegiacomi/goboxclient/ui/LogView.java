package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.sync.Work;
import org.apache.log4j.Logger;

import java.util.Set;

public class LogView implements View {

    private static final Logger log = Logger.getLogger(LogView.class);

    private Presenter presenter;

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setClient(GBClient client) {

    }

    @Override
    public void setCurrentWorks(Set<Work> worksQueue) {

    }

    @Override
    public void setMessage(String message) {
        log.info(message);
    }

    @Override
    public void showError(String error) {
        log.warn(error);
    }
}
