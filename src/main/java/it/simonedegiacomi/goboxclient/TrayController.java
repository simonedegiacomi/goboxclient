package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.apache.log4j.Logger;

import javax.accessibility.AccessibleRelation;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * Created on 20/01/16.
 * @author Degiacomi Simone
 */
public class TrayController {

    // Default icon file name
    private static final String ICON_NAME = "/it/simonedegiacomi/resources/icon.png";

    private final static URLBuilder urls = Config.getInstance().getUrls();

    // System icon tray to add icons
    private final static SystemTray tray = SystemTray.getSystemTray();

    // Desktop object used to object the browser
    private final Desktop desktop;

    private PopupMenu trayMenu = new PopupMenu();

    // Menu items
    private MenuItem state, mode, exit, connSettings;

    private CheckboxMenuItem syncCheck;

    /**
     * Create a new tray controller without showing anything
     */
    public TrayController () {
        desktop = Desktop.getDesktop();
        addButtons();
    }

    /**
     * Add buttons and prepare labels for the menu icon tray
     */
    private void addButtons() {
        // Client settings, this open the connection
        // window
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
     * show the icon in the system icon tray
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

    /**
     * Change the 'state' label. Useful to communicate silent messages
     * to the user.
     * @param message Message to show
     */
    public void setMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                state.setLabel(message);
            }
        });
    }

    /**
     * Change the mode label
     * @param modeName Name of the current mode
     */
    public void setMode (String modeName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mode.setLabel("Mode: " + modeName);
            }
        });
    }

    /**
     * Set the click listener on the 'exit' label
     * @param listener Listener for the click
     */
    public void setOnCloseListener (ActionListener listener) {
        exit.addActionListener(listener);
    }

    /**
     * Set the listener for the settings label.
     * @param listener Listener for the click
     */
    public void setSettingsButtonListener (ActionListener listener) { connSettings.addActionListener(listener); }

    public void setSyncCheckUsability(boolean usability) {
        syncCheck.setEnabled(usability);
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