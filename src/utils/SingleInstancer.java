package utils;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by simonedegiacomi on 10/01/16.
 */
public class SingleInstancer {

    private static final int DEFAULT_PORT = 4212;

    private final int port;
    private final boolean single = false

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
                ServerSocket server = new ServerSocket(port);
                for(;;)
                    try {
                        server.accept();
                    } catch (Exception ex) {}
            }
        }).start();
    }
}
