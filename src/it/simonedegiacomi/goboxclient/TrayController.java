package it.simonedegiacomi.goboxclient;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.sync.Sync;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by simone onEvent 20/01/16.
 */
public class TrayController {

    private static final Logger log = Logger.getLogger(TrayController.class.getName());

    private final static SystemTray tray = SystemTray.getSystemTray();

    private static final String ICON_NAME = "/it/simonedegiacomi/resources/icon.png";

    private final Desktop desktop;

    private final static URLBuilder urls = Config.getInstance().getUrls();

    private MenuItem state, mode;

    private Sync sync;

    private PopupMenu trayMenu = new PopupMenu();

    public TrayController (Sync sync) {
        this.sync = sync;
        addButtons();
        desktop = Desktop.getDesktop();
    }

    private void addButtons () {
        // Client settings, this open the connection
        // window
        MenuItem connSettings = new MenuItem();
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
        CheckboxMenuItem syncCheck = new CheckboxMenuItem();
        syncCheck.setLabel("Sync folders");
        syncCheck.setState(true);

        trayMenu.add(syncCheck);

        mode = new MenuItem();
        mode.setLabel("Mode");
        trayMenu.add(mode);

        state = new MenuItem();
        state.setLabel("State");
        state.setEnabled(false);
        trayMenu.add(state);

        trayMenu.addSeparator();

        MenuItem exit = new MenuItem();
        exit.setLabel("Exit");
        trayMenu.add(exit);
    }

    public void showTray () {
        try {
            // Load th icon for the tray
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
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }
}
