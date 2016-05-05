package it.simonedegiacomi.configuration.loginui;

/**
 * Created on 23/04/16.
 * @author Degiacomi Simone
 */
public interface LoginView {

    void setPresenter (LoginPresenterInterface presenter);

    void setUsername (String username);

    void setUseAsStorage (boolean storage);

    String getUsername ();

    char[] getPassword ();

    boolean getUseAsStorage();

    void close();

    void show();

    void setLoading(boolean b);

    void showError(String error);
}