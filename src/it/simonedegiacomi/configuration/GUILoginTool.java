package it.simonedegiacomi.configuration;

import it.simonedegiacomi.goboxapi.authentication.Auth;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Degiacomi Simone on 02/01/2016.
 */
public class GUILoginTool extends LoginTool {

    private final Config config = Config.getInstance();

    private GUIConnectionTool proxyWindow;

    public GUILoginTool(EventListener after) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter your username: "));
        JTextField username = new JTextField(10);
        panel.add(username);
        panel.add(new JLabel("Enter a password:" ));
        JPasswordField pass = new JPasswordField(10);
        panel.add(pass);
        JCheckBox storageCheck = new JCheckBox("Use as Storage");
        panel.add(storageCheck);
        JButton showProxyConfig = new JButton("Settings");
        panel.add(showProxyConfig);
        showProxyConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(proxyWindow == null) {
                    proxyWindow = new GUIConnectionTool();
                    proxyWindow.show();
                }
            }
        });
        String[] options = new String[]{"Login", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "GoBox login",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if(option != JOptionPane.OK_OPTION)
            System.exit(-1);
        // Try to login
        Auth auth = new Auth(username.getText());
        auth.setPassword(new String(pass.getPassword()));
        auth.setMode(storageCheck.isSelected() ? Auth.Modality.STORAGE_MODE : Auth.Modality.STORAGE_MODE);
        try {
            auth.login();
            config.save();
            after.onLoginComplete();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Login failed");
            System.exit(-1);
        }
    }
}
