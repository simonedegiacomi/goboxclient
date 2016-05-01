package it.simonedegiacomi.configuration.loginui;

import it.simonedegiacomi.goboxclient.ui.Presenter;

import java.io.Console;

/**
 * Created on 23/04/16.
 * @author Degiacomi Simone
 */
public class ConsoleLoginView implements LoginView {

    private LoginPresenterInterface presenter;

    private final Console console = System.console();

    private String username;

    private char[]  password;

    private boolean useAsStorage;

    @Override
    public void show () {

        console.printf("Console Login Tool\n");

        // Ask for the username
        console.printf("Username: ");
        username = console.readLine();

        // Ask for the password
        console.printf("Password: ");
        password = console.readPassword();

        // Use as storage
        console.printf("Use as?\n1) Client\n2) Storage\n");
        String temp = console.readLine();
        useAsStorage = temp.startsWith("2");

        presenter.login();
    }

    @Override
    public void setLoading(boolean loading) {
       if (loading)
           console.printf("Authenticating...\n");
    }

    @Override
    public void showError() {
        console.printf("Invalid login credentials!\nRetry? [Y/n]\n");
        if(console.readLine().equalsIgnoreCase("y"))
            show();
        else
            presenter.cancel();
    }

    @Override
    public void setPresenter(LoginPresenterInterface presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setUsername(String username) { }

    @Override
    public void setUseAsStorage(boolean storage) { }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public char[] getPassword() {
        return password;
    }

    @Override
    public boolean getUseAsStorage() {
        return useAsStorage;
    }

    @Override
    public void close() {
        console.printf("Login terminated\n");
    }
}
