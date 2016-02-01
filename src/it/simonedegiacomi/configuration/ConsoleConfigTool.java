package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;

import java.io.Console;

/**
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class ConsoleConfigTool extends ConfigTool{

    private Console console = System.console();
    private final Config config = Config.getInstance();
    private char[] password;

    public ConsoleConfigTool(EventListener callback) {

        // Ask for the config
        console.printf("Hi, this is the ConsoleConfigTool of GoBoxClient\n");
        console.printf("First, i need the credentials for your account.\n");
        console.printf("Username: ");
        String username = console.readLine();
        console.printf("Password: ");
        password = console.readPassword();
        boolean isNumber = false;
        int n = 0;
        while(!isNumber) {
            console.printf("Would you like to use this computer as a client or as a Storage (Remember " +
                "you can assign only one storage for account!)\n1) StandardClient mode;\n2) Storage mode;\n");
            String modeString = console.readLine();
            try {
                n = Integer.parseInt(modeString);
                isNumber = !(n != 1 && n!= 2);
            } catch (Exception ex) {
                console.printf("Sorry, only numbers are accepted");
            }
        }
        console.printf("Connecting to GoBoxServer...  ");
        Auth auth = new Auth(username);
        auth.setPassword(password);
        auth.setMode(n - 1);
        try {
            auth.login();
        } catch (AuthException ex) {
            console.printf(" Done with errors\nGoBoxServer rejected your login request\n");
            console.printf("Response: %s", ex.getMessage());
            callback.onConfigFailed();
            return;
        }
        config.setMode(n - 1);
        config.setProperty("token", auth.getToken());
        config.setProperty("username", auth.getUsername());
        console.printf(" Done!\n");
        callback.onConfigComplete();
    }
}