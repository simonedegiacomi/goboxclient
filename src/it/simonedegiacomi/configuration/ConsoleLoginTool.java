package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;

import java.io.Console;

/**
 * Created by Degiacomi Simone onEvent 31/12/2015.
 */
public class ConsoleLoginTool extends LoginTool {

    private Console console = System.console();
    private final Config config = Config.getInstance();
    private char[] password;

    public ConsoleLoginTool(EventListener callback) {

        // Ask for the config
        console.printf("Hi, this is the ConsoleLoginTool of GoBoxClient\n");
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
        auth.setPassword(new String(password));
        auth.setMode(n == 1 ? Auth.Modality.CLIENT_MODE : Auth.Modality.STORAGE_MODE);
        try {
            auth.login();
            config.save();
        } catch (AuthException ex) {
            console.printf(" Done with errors\nGoBoxServer rejected your login request\n");
            console.printf("Response: %s", ex.getMessage());
            callback.onLoginFailed();
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        console.printf(" Done!\n");
        callback.onLoginComplete();
    }
}