package it.simonedegiacomi.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to check if the program
 * is running in single instance mode
 *
 * Created by Degiacomi Simone on 10/01/16.
 */
public class SingleInstancer {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(SingleInstancer.class.getName());

    /**
     * Default port used to check if another instance
     * is running on the same machine
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
     * Create a new single instancer and check if is
     * the only instance in the system
     * @param port Port to use
     */
    public SingleInstancer (int port) {
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
    public SingleInstancer () {
        this(DEFAULT_PORT);
    }

    public boolean isSingle () {
        return single;
    }

    /**
     * Try to conenct to another instance
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
                    for (; ; )
                        try {
                            server.accept();
                        } catch (Exception ex) {
                        }
                } catch (IOException ex) {
                    log.log(Level.WARNING, ex.toString(), ex);
                }
            }
        }).start();
    }
}