package it.simonedegiacomi.configuration.loginui;

/**
 * Created on 23/04/16.
 * @author Degiacomi Simone
 */
public interface LoginModelInterface {

    void setUsername (String username);

    void setPassword (char password[]);

    void setUseAsStorage (boolean storage);

    String getUsername ();

    char[] getPassword ();

    boolean getUseAsStorage ();

    boolean check();
}
