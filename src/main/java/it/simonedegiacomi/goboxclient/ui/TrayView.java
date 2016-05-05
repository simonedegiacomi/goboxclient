package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.configuration.loginui.GUIConnectionTool;
import it.simonedegiacomi.goboxapi.client.Client;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.sync.Work;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

/**
 *
 * Created on 20/01/16.
 * @author Degiacomi Simone
 */
public class TrayView implements View {

    /**
     * Logger of the class
     */
    private final static Logger logger = Logger.getLogger(TrayView.class);

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
    private MenuItem messageItem, stateItem, modeItem, exitItem, connSettingsItem;

    private CheckboxMenuItem syncCheck;

    /**
     * Create a new tray controller without showing anything
     */
    public TrayView (Presenter presenter) {
        this.presenter = presenter;
        desktop = Desktop.getDesktop();
        trayMenu.setName("GoBox");
        addButtons();
    }

    /**
     * Add buttons and prepare labels for the menu icon tray
     */
    private void addButtons() {
        // Client settings, this open the connection window
        connSettingsItem = new MenuItem();
        connSettingsItem.setLabel("Client Settings");
        connSettingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUIConnectionTool.show();
            }
        });
        trayMenu.add(connSettingsItem);

        // This open the browser to GoBox WebApp page
        MenuItem accountSettings = new MenuItem();
        accountSettings.setLabel("Account Settings");
        accountSettings.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    desktop.browse(urls.getURI("webapp"));
                } catch (IOException ex) {
                    logger.warn(ex.toString(), ex);
                }
            }
        });
        trayMenu.add(accountSettings);

        // Sync option
        trayMenu.addSeparator();

        // Checkbox that user use to start and stop the cynchronization
        syncCheck = new CheckboxMenuItem();
        syncCheck.setLabel("Sync folders");
        syncCheck.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                presenter.setSync(syncCheck.getState());
            }
        });
        syncCheck.setEnabled(false);
        syncCheck.setState(true);

        trayMenu.add(syncCheck);

        modeItem = new MenuItem();
        trayMenu.add(modeItem);

        messageItem = new MenuItem();
        trayMenu.add(messageItem);

        stateItem = new MenuItem();
        trayMenu.add(stateItem);

        trayMenu.addSeparator();

        // Exit button
        exitItem = new MenuItem();
        exitItem.setLabel("Exit");
        exitItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                presenter.exitProgram();
            }
        });
        trayMenu.add(exitItem);

        // Load the icon
        URL iconUrl = getClass().getResource(ICON_NAME);
        final TrayIcon icon = new TrayIcon(new ImageIcon(iconUrl).getImage());
        icon.setToolTip("GoBox");
        icon.setImageAutoSize(true);

        // Attach the menu to the icon
        icon.setPopupMenu(trayMenu);

        // Add the icon to the system tray
        SwingUtilities.invokeLater(() -> {
            try {
                tray.add(icon);
            } catch (AWTException ex) {
                logger.warn(ex);
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
    public void setClientState(Client.ClientState state) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stateItem.setName("State: " + state);
            }
        });
    }

    @Override
    public void setSyncState(boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                syncCheck.setState(enabled);
            }
        });
    }

    @Override
    public void setCurrentWorks(Set<Work> worksQueue) {
        // TODO: Implement works count or list
    }

    @Override
    public void setMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageItem.setName(message);
            }
        });
    }

    @Override
    public void showError(String error) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}