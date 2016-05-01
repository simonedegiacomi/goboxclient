package it.simonedegiacomi.configuration;

import it.simonedegiacomi.configuration.loginui.*;

import java.awt.*;

/**
 * Created on 4/23/16.
 * @author Degiacomi Simone
 */
public class LoginTool {

    public static void startLogin (Runnable onLogin, Runnable onCancel) {
        LoginModelInterface model = new LoginModel();
        LoginPresenterInterface presenter = new LoginPresenter(onLogin, onCancel);
        presenter.setModel(model);

        LoginView view;
        if (GraphicsEnvironment.isHeadless()) {
            view = new ConsoleLoginView();
        } else {
            view = new SwingLoginView();
        }

        view.setPresenter(presenter);
        presenter.setView(view);
        presenter.startLogin();
    }
}
