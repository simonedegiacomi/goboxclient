package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.sync.Work;
import org.apache.log4j.Logger;

import java.util.Set;

public class LogView implements View {

    private static final Logger log = Logger.getLogger(LogView.class);

    private Presenter presenter;

    private GoBoxEnvironment env;

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setEnvironment(GoBoxEnvironment env) {
        this.env = env;
    }

    @Override
    public void setMessage(String message) {
        log.info(message);
    }

    @Override
    public void showError(String error) {
        log.warn(error);
    }

    @Override
    public void updateViewFromEnvironment() {
        log.info("Works: " + env.getSync().getWorkManager().getQueueSize());
    }
}
