package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.configuration.loginui.GUIConnectionTool;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.sync.Work;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

/**
 * this view add an icon tray
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
    private final static URLBuilder urls = URLBuilder.DEFAULT;

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
     * Mode item
     */
    private MenuItem modeItem;

    /**
     * Works count item
     */
    private MenuItem worksCountItem;

    private MenuItem exitItem, connSettingsItem;

    /**
     * Create a new tray controller without showing anything
     */
    public TrayView(Presenter presenter) {
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

        modeItem = new MenuItem();
        modeItem.setLabel("Starting...");
        modeItem.setEnabled(false);
        trayMenu.add(modeItem);

        worksCountItem = new MenuItem();
        worksCountItem.setLabel("Starting...");
        worksCountItem.setEnabled(false);
        trayMenu.add(worksCountItem);

        trayMenu.addSeparator();

        // Exit button
        exitItem = new MenuItem();
        exitItem.setLabel("Exit");
        exitItem.addActionListener((event) -> presenter.exitProgram());
        trayMenu.add(exitItem);

        // Load the icon
        URL iconUrl = getClass().getResource(ICON_NAME);
        final TrayIcon icon = new TrayIcon(new ImageIcon(iconUrl).getImage());
        icon.setToolTip("GoBox");
        icon.setImageAutoSize(true);

        // Attach the menu to the icon
        icon.setPopupMenu(trayMenu);

        // Add the mouse listener
        icon.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                presenter.updateView();
            }

            @Override
            public void mousePressed(MouseEvent e) { }

            @Override
            public void mouseReleased(MouseEvent e) { }

            @Override
            public void mouseEntered(MouseEvent e) { }

            @Override
            public void mouseExited(MouseEvent e) { }
        });

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
    public void setClient(GBClient client) {
        SwingUtilities.invokeLater(() -> {
            if (presenter.isStorage()) {
                modeItem.setLabel("Storage");
                return;
            }
            modeItem.setLabel(((StandardGBClient) client).getCurrentTransferProfile().getMode().toString());
        });
    }

    @Override
    public void setCurrentWorks(Set<Work> worksQueue) {
        SwingUtilities.invokeLater(() -> worksCountItem.setLabel("Works: " + worksQueue.size()));
    }

    @Override
    public void setMessage(String message) {
        System.out.println(message);
    }


    @Override
    public void showError(String error) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE));
    }
}