package configuration;

import goboxapi.authentication.Auth;

import javax.swing.*;

/**
 * Created by Simone on 02/01/2016.
 */
public class GUIConfigTool extends ConfigTool {

    public GUIConfigTool (Config config, EventListener after) {
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
        String[] options = new String[]{"Login", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "GoBox login",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if(option != JOptionPane.OK_OPTION)
            System.exit(-1);
        // Try to login
        Auth auth = new Auth(username.getText());
        auth.setPassword(pass.getPassword());
        auth.setMode(storageCheck.isSelected() ? Auth.STORAGE : Auth.CLIENT);
        try {
            auth.login();
            config.setMode(storageCheck.isSelected() ? Config.STORAGE_MODE : Config.CLIENT_MODE);
            config.setProperty("username", auth.getUsername());
            config.setProperty("token", auth.getToken());
            after.onConfigComplete();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Login failed");
            System.exit(-1);
        }
    }
}
