package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;

import java.io.Console;

/**
 * Created on 31/12/2015.
 * @author Degiacomi Simone
 */
public class ConsoleLoginTool extends LoginTool {

    /**
     * Console of the system, used to read the password
     */
    private Console console = System.console();

    /**
     * Configuration instance
     */
    private final Config config = Config.getInstance();

    private final EventListener callback;

    /**
     * Create the console configuration tool and start the login procedure
     * @param callback
     */
    public ConsoleLoginTool(EventListener callback) {

        this.callback = callback;

        startProcedure();
    }

    private void startProcedure () {

        // Ask for the config
        console.printf("Hi, this is the ConsoleLoginTool of GoBoxClient\n");
        console.printf("First, i need the credentials for your account.\n");

        // Ask for the username
        console.printf("Username: ");

        // Read the username
        String username = console.readLine();

        // ask for the password
        console.printf("Password: ");

        // RRead the password
        char[] password = console.readPassword();

        // Read the selected mode (client or storage)
        boolean isNumber = false;
        int n = 0;
        while(!isNumber) {

            // Ask for the mode
            console.printf("Would you like to use this computer as a client or as a Storage (Remember " +
                    "you can assign only one storage for account!)\n1) StandardClient mode;\n2) Storage mode;\n");

            // Read the user input
            String modeString = console.readLine();

            try {

                // Cast to integer
                n = Integer.parseInt(modeString);
                isNumber = !(n != 1 && n!= 2);
            } catch (Exception ex) {
                console.printf("Sorry, only numbers are accepted");
            }
        }

        console.printf("Connecting to GoBoxServer...  ");

        // Create a new auth object
        Auth auth = config.getAuth();
        auth.setUsername(username);
        auth.setMode(n == 1 ? Auth.Modality.CLIENT : Auth.Modality.STORAGE);

        // Login
        try {

            // Try to login
            boolean logged = auth.login(new String(password));

            if (logged) {

                // Save the configuration
                config.save();

                console.printf("Done!\n");

                // call the listener
                callback.onLoginComplete();

                return;
            } else {

                // Login failed
                console.printf("Login failed: Wrong username or password\n");

                // Login failed
                callback.onLoginFailed();
            }
        } catch (Exception ex) {

            // Show the error
            console.printf("Done with errors\nGoBoxServer rejected your login request\n");
            console.printf("Response: %s", ex.getMessage());

            // Login failed
            callback.onLoginFailed();
        }
    }
}