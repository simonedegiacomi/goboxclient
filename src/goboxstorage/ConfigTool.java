package goboxstorage;

import javax.swing.*;
import java.awt.*;
import java.io.Console;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public class ConfigTool {

    private final Config config;
    private char[] password;

    public ConfigTool (Config config) {
        this.config = config;
        if (GraphicsEnvironment.isHeadless()) {
            shellConfig();
        } else {
            guiConfig();
        }
    }

    private void guiConfig () {
        JPanel panel = new JPanel();
        panel.add(new JLabel("GoBoxStorage: Account"));
        panel.add(new JLabel("Insert you GoBox account username:"));
        JTextField username = new JTextField(10);
        panel.add(username);
        panel.add(new JLabel("Enter the password:"));
        JPasswordField pass = new JPasswordField(10);
        panel.add(pass);
        String[] options = new String[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "GoBoxStorage",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);

        if(option == JOptionPane.OK_OPTION) {
            password = pass.getPassword();
            config.setProperty("username", username.getText());
        }
    }

    private void shellConfig () {
        Console console = System.console();
        System.out.println("Hi, i'm the configuratio tool for the GoBoxServer!\nPlease, enter your GoBox username");
        String username = console.readLine();
        System.out.println("Ok " + username + ", now enter your password:");
        password = console.readPassword();
        System.out.println("Perfect, the last question: where should i put the files?");
        String path = console.readLine();
        System.out.println("Done, the GoBoxStorage will be ready in few seconds!");

        // Set the config
        config.setProperty("username", username);
        config.setProperty("goBoxPath", path);
    }

    public char[] getPassword () {
        return password;
    }
}
