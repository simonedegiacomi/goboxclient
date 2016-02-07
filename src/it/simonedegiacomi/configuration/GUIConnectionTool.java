package it.simonedegiacomi.configuration;

import javax.swing.*;

/**
 * Created by Degiacomi Simone onEvent 06/02/16.
 */
public class GUIConnectionTool {

    private static final Config config = Config.getInstance();
    private JPanel panel;
    private JCheckBox proxyCheck;
    private JTextField ipField, portField;

    public GUIConnectionTool () {
        panel = new JPanel();
        panel.add((proxyCheck = new JCheckBox("Use proxy")));
        panel.add(new JLabel("Proxy IP"));
        panel.add(new JLabel("Port"));
        panel.add((ipField = new JTextField()));
        panel.add((portField = new JTextField()));
    }

    public void show () {

        String[] options = new String[]{ "Save", "Cancel" };

        int option = JOptionPane.showOptionDialog(null, panel, "GoBox - Connection",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if(option != JOptionPane.OK_OPTION)
            return;
        config.setProperty("useProxy", String.valueOf(proxyCheck.isSelected()));
        config.setProperty("proxyIP", ipField.getText());
        config.setProperty("proxyPort", portField.getText());
    }
}
