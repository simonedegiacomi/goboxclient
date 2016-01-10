package goboxclient;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by simonedegiacomi on 09/01/16.
 */
public class GoBoxIconTray {

    private static final String ICON_NAME = "";

    private static final Logger log = Logger.getLogger(GoBoxIconTray.class.getName());

    public static boolean isSupported () {
        return SystemTray.isSupported();
    }

    public GoBoxIconTray () {

        // Get th eteeay of the system
        SystemTray tray = SystemTray.getSystemTray();

        // Create the menu that contains the items
        PopupMenu menu = new PopupMenu();

        try {
            // Load th icon for the tray
            URL iconUrl = getClass().getResource(ICON_NAME);
            TrayIcon icon = new TrayIcon(new ImageIcon(iconUrl));

            // Set the menu to the icon
            icon.setPopupMenu(menu);

            // Add the iconTray to the system tray
            tray.add(icon);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }
}