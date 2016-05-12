package it.simonedegiacomi.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxclient.ui.CLIView;
import it.simonedegiacomi.goboxclient.ui.Presenter;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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

    private final Gson gson = MyGsonBuilder.create();

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
                            new CLIView(presenter, console);
                        } catch (Exception ex) {
                            log.warn(ex.toString(), ex);
                        }
                } catch (IOException ex) {
                    log.warn(ex.toString(), ex);
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

        // Create a json array with the arguments
        JsonArray jsonArgs = new JsonArray();
        for (String arg : args) {
            jsonArgs.add(arg);
        }

        // Send the arguments
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(mainInstance.getOutputStream()));
        gson.toJson(jsonArgs, writer);
        writer.flush();
        mainInstance.shutdownOutput();

        // Read the response
        JsonObject res = new JsonParser().parse(new JsonReader(new InputStreamReader(mainInstance.getInputStream()))).getAsJsonObject();

        // Print the output
        System.out.println(res.get("out").getAsString());

        // Close the reader
        mainInstance.close();

        // Stop the program
        System.exit(0);
    }
}