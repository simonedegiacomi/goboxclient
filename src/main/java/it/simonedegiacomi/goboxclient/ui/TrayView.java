package it.simonedegiacomi.goboxclient.ui;

import it.simonedegiacomi.configuration.loginui.GUIConnectionTool;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxapi.client.StandardGBClient;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.sync.Work;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

/**
 * this view add an trayIcon tray
 * Created on 20/01/16.
 * @author Degiacomi Simone
 */
public class TrayView implements View {

    /**
     * Logger of the class
     */
    private final static Logger logger = Logger.getLogger(TrayView.class);

    /**
     * Default trayIcon file name
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
     * System trayIcon tray to add icons
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

    private TrayIcon trayIcon;

    private GoBoxEnvironment env;


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
     * Add buttons and prepare labels for the menu trayIcon tray
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

        // Load the trayIcon
        URL iconUrl = getClass().getResource(ICON_NAME);
        trayIcon = new TrayIcon(new ImageIcon(iconUrl).getImage());
        trayIcon.setToolTip("GoBox");
        trayIcon.setImageAutoSize(true);

        // Attach the menu to the trayIcon
        trayIcon.setPopupMenu(trayMenu);

        // Add the mouse listener
        trayIcon.addMouseListener(new MouseListener() {

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

        // Add the trayIcon to the system tray
        SwingUtilities.invokeLater(() -> {
            try {
                tray.add(trayIcon);
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
    public void setEnvironment(GoBoxEnvironment env) {
        this.env = env;
    }

    @Override
    public void setMessage(String message) {
        trayIcon.displayMessage(null, message, TrayIcon.MessageType.INFO);
    }


    @Override
    public void showError(String error) {
        JOptionPane.showMessageDialog(null, "GoBox - Error", error, JOptionPane.ERROR_MESSAGE);
        trayIcon.displayMessage(null, error, TrayIcon.MessageType.ERROR);
    }

    @Override
    public void updateViewFromEnvironment() {
        String mode = env.getAuth().getMode().toString();
        if (env.getAuth().getMode() == GBAuth.Modality.CLIENT && env.getClient() instanceof StandardGBClient) {
            mode += " " + ((StandardGBClient)env.getClient()).getCurrentTransferProfile().getMode();
        }
        modeItem.setLabel(mode);
        worksCountItem.setLabel("Works: " + env.getSync().getWorkManager().getQueueSize());
    }
}