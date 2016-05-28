package it.simonedegiacomi.configuration.loginui;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.utils.EasyProxy;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Very simple dialog to ask the user the proxy settings.
 * Created on 06/02/16.
 * @author Degiacomi Simone
 */
public class GUIConnectionTool {

    private static final Config config = Config.getInstance();

    private static JPanel panel;

    private static JCheckBox proxyCheck;

    private static JTextField ipField, portField;

    private volatile static boolean visible = false;

    private static void init () {
        // Main dialog panel
        panel = new JPanel();

        // Add ip field
        panel.add(new JLabel("Proxy IP"));
        panel.add((ipField = new JTextField(10)));

        // Add port field
        panel.add(new JLabel("Port"));
        panel.add((portField = new JTextField(5)));

        // Proxy check box
        panel.add((proxyCheck = new JCheckBox("Use proxy")));
        proxyCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ipField.setEnabled(proxyCheck.isSelected());
                portField.setEnabled(proxyCheck.isSelected());
            }
        });
    }

    public static void show () {

        // Si the dialog already visible?
        if (visible) {
            return;
        }

        visible = true;

        // Assert that the dialog components are ready
        if (panel == null) {
            init();
        }

        // Configure components with the current config
        updateFromConfig();

        // Dialog options
        String[] options = new String[]{ "Save", "Cancel" };

        // Show dialog
        int option = JOptionPane.showOptionDialog(null, panel, "GoBox - Connection",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if(option != JOptionPane.OK_OPTION)
            return;

        // Update config
        config.setProperty("useProxy", String.valueOf(proxyCheck.isSelected()));
        config.setProperty("proxyIP", ipField.getText());
        config.setProperty("proxyPort", portField.getText());
        try {
            config.save();
        } catch (IOException ex) { }


        visible = false;

        EasyProxy.handleProxy(config);
    }

    private static void updateFromConfig () {
        if (config.hasProperty("proxyIP")) {
            ipField.setText(config.getProperty("proxyIP"));
        }

        if (config.hasProperty("proxyPort")) {
            portField.setText(config.getProperty("proxyPort"));
        }

        boolean useProxy = config.hasProperty("useProxy") && Boolean.parseBoolean(config.getProperty("useProxy"));
        proxyCheck.setSelected(useProxy);
        portField.setEnabled(useProxy);
        ipField.setEnabled(useProxy);
    }
}