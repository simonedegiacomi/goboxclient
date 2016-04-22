package it.simonedegiacomi.utils;

import it.simonedegiacomi.goboxclient.ui.CLIView;
import it.simonedegiacomi.goboxclient.ui.Presenter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to check if the program
 * is running in single instance mode
 *
 * Created by Degiacomi Simone onEvent 10/01/16.
 */
public class GoBoxInstance {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(GoBoxInstance.class.getName());

    /**
     * Default port used to check if another instance
     * is running onEvent the same machine
     */
    private static final int DEFAULT_PORT = 4212;

    /**
     * Port used in this instance
     */
    private final int port;

    private Socket mainInstance;

    /**
     * Presenter used to which add the view
     */
    private Presenter presenter;

    /**
     * Create a new single instancer and check if is
     * the only instance in the system
     * @param port Port to use
     */
    public GoBoxInstance(int port) {
        this.port = port;

        // Try to connect to another instance
        try {
            mainInstance = new Socket("localhost", port);
        } catch (Exception ex) {

            // If i cannot contact any server, i'm the only instance
            startLighthouse();
            return;
        }
    }

    /**
     * Create a new single instancer and check if is
     * the only instance in the system, using the default
     * port
     */
    public GoBoxInstance() {
        this(DEFAULT_PORT);
    }

    public boolean isSingle () {
        return mainInstance == null;
    }


    /**
     * Start the server that will accept incoming
     * socket from other instance
     */
    private void startLighthouse () {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(port);
                    for (;;)
                        try {

                            // Get the socket from another instance of gobox
                            Socket console = server.accept();

                            // Create a new CLI view
                            CLIView cli = new CLIView(presenter, console);

                            // Add the view to the presenter
                            presenter.addView(cli);
                        } catch (Exception ex) {}
                } catch (IOException ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        }).start();
    }

    /**
     * Set the presenter to which add the cli views
     * @param presenter Presenter to use
     */
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public void sendToMainInstance(String[] args) throws IOException {

        // Create the writer to the main instance
        PrintWriter toMainInstance = new PrintWriter(mainInstance.getOutputStream());

        // Create the reader from the main instance
        BufferedReader fromMainInstance = new BufferedReader(new InputStreamReader(mainInstance.getInputStream()));

        // Write the args to the main instance
        // First the number of args
        toMainInstance.println(args.length);

        // The all the arguments
        for (String arg : args)
            toMainInstance.println(arg);

        // Close the writer
        toMainInstance.close();

        // Read from the main instance
        String string;
        while (!(string = fromMainInstance.readLine()).equals("END"))
            System.out.println(string);

        // Close the reader
        fromMainInstance.close();
        mainInstance.close();

        // Stop the program
        System.exit(0);
    }
}