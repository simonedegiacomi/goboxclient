package configuration;

import goboxapi.authentication.Auth;
import goboxapi.authentication.AuthException;

import java.io.Console;

/**
 * Created by Degiacomi Simone on 31/12/2015.
 */
public class ConsoleConfigTool extends ConfigTool{

    private Console console = System.console();
    private char[] password;

    public ConsoleConfigTool(Config config, EventListener callback) {

        // Ask for the config
        console.printf("Hi, this is the ConsoleConfigTool oif GoBoxClient\n");
        console.printf("First, i need the credentials for your account.\n");
        console.printf("Username: ");
        String username = console.readLine();
        console.printf("Password:");
        password = console.readPassword();
        boolean isNumber = false;
        int n;
        while(!isNumber) {
            console.printf("Would you like to use this computer as a client or as a Storage (Remember " +
                "you can assign only one storage for account!)\n1) StandardClient mode;\n2) Storage mode;");
            String modeString = console.readLine();
            try {
                n = Integer.parseInt(modeString);
                isNumber = !(n != 1 && n!= 2);
            } catch (Exception ex) {
                console.printf("Sorry, only numbers are accepted");
            }
        }
        console.printf("Connecting to GoBoxServer... ");
        Auth auth = new Auth(username);
        auth.setPassword(password);
        try {
            auth.login();
        } catch (AuthException ex) {
            console.printf("Done with errors\nGoBoxServer rejected your login request\n");
            console.printf("Response: %s", ex.getMessage());
            callback.onConfigFailed();
            return;
        }
        console.printf("Done!\n");
        callback.onConfigComplete();
    }
}
