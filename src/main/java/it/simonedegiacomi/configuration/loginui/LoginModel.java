package it.simonedegiacomi.configuration.loginui;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;

import java.io.IOException;

/**
 * Created on 4/23/16.
 * @author Degiacomi Simone
 */
public class
LoginModel implements LoginModelInterface {

    private final static Config config = Config.getInstance();

    private String username;

    private char[] password;

    private boolean useAsStorage;

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setPassword(char[] password) {
        this.password = password;
    }

    @Override
    public void setUseAsStorage(boolean storage) {
        this.useAsStorage = storage;
    }

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
    public boolean check() throws IOException {
        GBAuth test = config.getAuth();
        test.setUsername(username);
        test.setMode(useAsStorage ? GBAuth.Modality.STORAGE : GBAuth.Modality.CLIENT);
        boolean logged = test.login(new String(password));
        if(logged)
            config.setAuth(test);
        return logged;
    }
}