package it.simonedegiacomi.utils;

import it.simonedegiacomi.goboxclient.ui.CLIView;
import it.simonedegiacomi.goboxclient.ui.Presenter;

import java.io.IOException;
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

    /**
     * State of execution
     */
    private final boolean single;

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
        single = checkIfSingle();
        if(single)
            startLighthouse();
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
        return single;
    }

    /**
     * Try to connect to another instance
     * @return state of running
     */
    private boolean checkIfSingle () {
        try {
            new Socket("localhost", port).close();
        } catch (Exception ex) {
            // If i cannot contact any server, i'm the only
            // instance
            return true;
        }

        // Another instance (or a server with this port.. ops)
        // is running.
        return false;
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
                            CLIView cli = new CLIView(console);

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
}