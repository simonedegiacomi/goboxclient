package utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Degiacomi Simone on 10/01/16.
 */
public class SingleInstancer {

    private static final Logger log = Logger.getLogger(SingleInstancer.class.getName());

    private static final int DEFAULT_PORT = 4212;

    private final int port;
    private final boolean single;

    public SingleInstancer (int port) {
        this.port = port;
        single = checkIfSingle();
        if(single)
            startLighthouse();
    }

    public SingleInstancer () {
        this(DEFAULT_PORT);
    }

    public boolean isSingle () {
        return single;
    }

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
