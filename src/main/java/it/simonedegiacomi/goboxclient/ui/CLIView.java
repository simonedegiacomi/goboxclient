package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.sync.Work;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    private PrintWriter toUser;

    /**
     * Read the arguments from the reader from the other instance socket
     * @param fromOtherInstance Reader from the other instance
     * @return Array of read arguments
     * @throws IOException
     */
    private static String[] readArgs (BufferedReader fromOtherInstance) throws IOException{

        // Read the number of arguments
        int n = Integer.parseInt(fromOtherInstance.readLine());
        String[] args = new String[n];
        for (int i = 0;i < n; i++)
            args[i] = fromOtherInstance.readLine();

        return args;
    }

    public CLIView (Presenter presenter, Socket socket) throws IOException {
        this.presenter = presenter;

        // read the args
        BufferedReader fromUser = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String args[] = readArgs(fromUser);

        // Create the print writer to the user
        toUser = new PrintWriter(socket.getOutputStream());
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
}