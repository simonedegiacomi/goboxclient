package it.simonedegiacomi.goboxclient.ui;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxclient.GoBoxFacade;
import it.simonedegiacomi.sync.Work;

import java.io.*;
import java.net.Socket;
import java.util.Set;

/**
 * This is the view for the other instance of the program. The other instance of the program communicate
 * with the main trough a local socket.
 * Created on 20/04/16.
 * @author Degiacomi Simone
 */
public class CLIView implements View {

    private Presenter presenter;

    private final Gson gson = MyGsonBuilder.create();

    /**
     * Read the arguments from the reader from the other instance socket
     * @param fromOtherInstance Reader from the other instance
     * @return Array of read arguments
     * @throws IOException
     */
    private static String[] readArgs (InputStreamReader in) throws IOException{

        // Read the json array
        JsonArray json = new JsonParser().parse(new JsonReader(in)).getAsJsonArray();

        String[] args = new String[json.size()];
        int i = 0;
        for(JsonElement arg : json) {
            args[i++] = arg.getAsString();
        }
        return args;
    }

    public CLIView (Presenter presenter, Socket socket) throws IOException {
        this.presenter = presenter;
        // read the args
        String args[] = readArgs(new InputStreamReader(socket.getInputStream()));

        JsonObject res = answer(args);

        // Send the result
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(socket.getOutputStream()));
        gson.toJson(res, writer);
        writer.close();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setClientState(Client.ClientState state) { }

    @Override
    public void setSyncState(boolean enabled) { }

    @Override
    public void setCurrentWorks(Set<Work> worksQueue) { }

    @Override
    public void setMessage(String message) { }

    @Override
    public void showError(String error) { }

    private JsonObject answer (String[] args) {

        // Prepare the response
        JsonObject res = new JsonObject();

        if (args.length <= 0) {
            res.addProperty("out", "GoBox already running");
            return res;
        }

        switch (args[0]) {
            case "stop":
                presenter.exitProgram();
                res.addProperty("out", "Closed");
            default:
                res.addProperty("out", getHelp());
                break;
        }

        return res;
    }

    private String getHelp () {
        return "GoBox CLI\nAvailable commands:\n" +
                "help) Show this list;\n" +
                "stop) Stop the main GoBox instance;\n" +
                "reset) Reset the GoBox environment;\n" +
                "status) Get the current status;\n";
    }
}