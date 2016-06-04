package it.simonedegiacomi.goboxclient.ui;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.sync.Work;
import it.simonedegiacomi.utils.EasyProxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
    public void setClient(GBClient client) {

    }

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
                break;
            case "proxy":
                res.addProperty("out", setProxy(args));
                break;
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
                "proxy) Set the http proxy" +
                "status) Get the current status;\n";
    }

    private String setProxy (String[] args) {
        if (args.length <= 1) {
            return "Proxy usage:" +
                    "gobox proxy ENABLED [HOST] [PORT]\n" +
                    " - ENABLED: Enable or no the http proxy ('enable' or 'disable'))\n" +
                    " - HOST: Host address of the http proxy\n" +
                    " - PORT: Port of the http proxy" ;
        }

        if (args.length == 2) {
            if(args[1].equalsIgnoreCase("disable")) {
                return "Proxy disabled";
            }
            return "Type 'gobox proxy' to get the proxy settings help";
        }

        Config.getInstance().setProperty("useProxy", "true");
        Config.getInstance().setProperty("proxyIP", args[2]);
        Config.getInstance().setProperty("proxyPort", args[3]);

        EasyProxy.handleProxy(Config.getInstance());

        return "Proxy set: http://" + args[2] + ":" + args[3];
    }
}