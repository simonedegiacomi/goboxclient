package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.sync.Work;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Set;

/**
 * Created on 20/01/16.
 * @author Degiacomi Simone
 */
public class TrayView implements View {

    /**
     * Default icon file name
     */
    private static final String ICON_NAME = "/icon.png";

    /**
     * URLBuilder used to get the url if the website
     */
    private final static URLBuilder urls = Config.getInstance().getUrls();

    /**
     * Presenter that set the data to this class and to which this class class
     * can proxy the user actions
     */
    private Presenter presenter;

    /**
     * System icon tray to add icons
     */
    private final static SystemTray tray = SystemTray.getSystemTray();

    /**
     * Desktop object used to object the browser
     */
    private final Desktop desktop;

    private PopupMenu trayMenu = new PopupMenu();

    /**
     * Menu items
     */
    private MenuItem state, mode, exit, connSettings;

    private CheckboxMenuItem syncCheck;

    /**
     * Create a new tray controller without showing anything
     */
    public TrayView (Presenter presenter) {
        this.presenter = presenter;
        desktop = Desktop.getDesktop();
        addButtons();
    }

    /**
     * Add buttons and prepare labels for the menu icon tray
     */
    private void addButtons() {
        // Client settings, this open the connection window
        connSettings = new MenuItem();
        connSettings.setLabel("Client Settings");
        trayMenu.add(connSettings);

        // This open the browser to GoBox WebApp page
        MenuItem accountSettings = new MenuItem();
        accountSettings.setLabel("Account Settings");
        accountSettings.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    desktop.browse(urls.getURI("webapp"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        trayMenu.add(accountSettings);

        // Sync option
        trayMenu.addSeparator();

        // checkbox that user use to start and stop the cynchronization
        syncCheck = new CheckboxMenuItem();
        syncCheck.setLabel("Sync folders");
        syncCheck.setEnabled(false);
        syncCheck.setState(true);

        trayMenu.add(syncCheck);

        mode = new MenuItem();
        mode.setLabel("Mode:");
        mode.setEnabled(false);
        trayMenu.add(mode);

        state = new MenuItem();
        state.setLabel("State:");
        state.setEnabled(false);
        trayMenu.add(state);

        trayMenu.addSeparator();

        // Exit button
        exit = new MenuItem();
        exit.setLabel("Exit");
        trayMenu.add(exit);
    }

    /**
     * Show the icon in the system icon tray
     */
    public void showTray() {
        // Load the icon for the tray
        URL iconUrl = getClass().getResource(ICON_NAME);
        final TrayIcon icon = new TrayIcon(new ImageIcon(iconUrl).getImage());

        // Set the menu to the icon
        icon.setPopupMenu(trayMenu);

        // Add the iconTray to the system tray
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    tray.add(icon);
                } catch (AWTException ex) {
                    ex.printStackTrace();
                }

            }
        });
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
    public void setClientState(ClientState state) {

    }

    @Override
    public void setSyncState(boolean enabled) {

    }

    @Override
    public void setWorks(Set<Work> worksQueue) {

    }

    /**
     * Interface for the listener of the sync label
     */
    public interface CheckStateListener {
        public void onChange(boolean newState);
    }

    /**
     * Set the listener for the check box state
     * @param listener Listener of the check box state
     */
    public void setSyncCheckListener (CheckStateListener listener) {
        syncCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onChange(syncCheck.getState());
            }
        });
    }
}