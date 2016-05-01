package it.simonedegiacomi.configuration.loginui;

/**
 * Created on 23/04/16.
 * @author Degiacomi Simone
 */
public interface LoginPresenterInterface {

    void setView (LoginView view);

    void setModel (LoginModelInterface model);

    void login ();

    void cancel ();

    void startLogin();
}
