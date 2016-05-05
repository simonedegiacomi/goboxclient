package it.simonedegiacomi.configuration.loginui;

import java.io.IOException;

/**
 * Created on 4/23/16.
 * @author Degiacomi Simone
 */
public class LoginPresenter implements LoginPresenterInterface {

    private LoginView view;

    private LoginModelInterface model;

    private final Runnable loginListener, cancelListener;

    public LoginPresenter (Runnable loginListener, Runnable cancelListener) {
        this.loginListener = loginListener;
        this.cancelListener = cancelListener;
    }

    @Override
    public void setView(LoginView view) {
        this.view = view;
    }

    @Override
    public void setModel(LoginModelInterface model) {
        this.model = model;
    }

    @Override
    public void login() {
        updateModelFromView();
        view.setLoading(true);
        try {
            if (model.check()) {
                view.setLoading(false);
                view.close();
                loginListener.run();
                return;
            }
            view.setLoading(false);
            view.showError("Invalid username or password");
        } catch (IOException ex) {
            view.setLoading(false);
            view.showError("Connection error");
        }
    }

    @Override
    public void cancel() {
        view.close();
        cancelListener.run();
    }

    @Override
    public void startLogin() {
        view.show();
    }

    private void updateModelFromView() {
        model.setUsername(view.getUsername());
        model.setPassword(view.getPassword());
        model.setUseAsStorage(view.getUseAsStorage());
    }

    private void updateViewFromModel () {
        view.setUsername(model.getUsername());
        view.setUseAsStorage(model.getUseAsStorage());
    }
}
