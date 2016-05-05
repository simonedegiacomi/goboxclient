package it.simonedegiacomi.configuration.loginui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created on 23/04/16.
 * @author Degiacomi Simone
 */
public class SwingLoginView implements LoginView {

    private LoginPresenterInterface presenter;

    private final JFrame window;

    private final JTextField username;

    private final JPasswordField password;

    private final JCheckBox useAsStorage;

    private final JButton cancel, login;

    private final JLabel loadingLabel, loadingSpinner;

    public SwingLoginView () {

        // Create the window
        window = new JFrame();
        window.setTitle("GoBox - Login");
        window.setLocationRelativeTo(null); // Center the window
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Create the pane
        JPanel mainPanel = new JPanel();
        window.add(mainPanel);
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.ipadx = 10;
        c.ipady = 8;

        // Username label
        c.gridx = c.gridy = 0;
        mainPanel.add(new JLabel("Username"), c);

        // Username input
        c.gridx = 1;
        c.gridy = 0;
        username = new JTextField(10);
        mainPanel.add(username, c);

        // Password field
        c.gridx = 0;
        c.gridy = 1;
        mainPanel.add(new JLabel("Password"), c);

        // Password input
        c.gridx = c.gridy = 1;
        password = new JPasswordField(10);
        mainPanel.add(password, c);

        // Use as storage
        c.gridx = 1;
        c.gridy = 2;
        useAsStorage = new JCheckBox("Use as Storage");
        mainPanel.add(useAsStorage, c);

        // Loading spinner
        c.gridx = 0;
        c.gridy = 3;
        loadingSpinner = new JLabel();
        loadingSpinner.setVisible(false);
        new Thread(() -> {
            ImageIcon icon = new ImageIcon(SwingLoginView.class.getClassLoader().getResource("loading_spinner.gif"));
            SwingUtilities.invokeLater(() -> loadingSpinner.setIcon(icon));
        }).start();
        mainPanel.add(loadingSpinner, c);

        // Loading label
        c.gridx = 1;
        c.gridy = 3;
        loadingLabel = new JLabel("Loading...");
        loadingLabel.setVisible(false);
        mainPanel.add(loadingLabel, c);

        // Cancel button
        c.gridx = 0;
        c.gridy = 4;
        cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                presenter.cancel();
            }
        });
        mainPanel.add(cancel, c);

        // Login button
        c.gridx = 1;
        c.gridy = 4;
        login = new JButton("Login");
        login.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread(() -> {
                    presenter.login();
                }).start();
            }
        });
        mainPanel.add(login, c);

        // Proxy settings button
        c.gridx = 0;
        c.gridy = 5;
        JButton proxySettings = new JButton("Proxy settings");
        proxySettings.addActionListener((ActionEvent evt) -> {
            GUIConnectionTool tool = new GUIConnectionTool();
            tool.show();
        });

        window.pack();
    }

    @Override
    public void setPresenter(LoginPresenterInterface presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setUsername(String name) {
        SwingUtilities.invokeLater(() -> username.setText(name));
    }

    @Override
    public void setUseAsStorage(boolean storage) {
        SwingUtilities.invokeLater(() -> useAsStorage.setSelected(storage));
    }

    @Override
    public String getUsername() {
        return username.getText();
    }

    @Override
    public char[] getPassword() {
        return password.getPassword();
    }

    @Override
    public boolean getUseAsStorage() {
        return useAsStorage.isSelected();
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> window.setVisible(false));
    }

    @Override
    public void show() {
        SwingUtilities.invokeLater(() -> window.setVisible(true));
    }

    @Override
    public void setLoading(boolean loading) {
        SwingUtilities.invokeLater(() -> {
                login.setEnabled(!loading);
                cancel.setEnabled(!loading);
                username.setEnabled(!loading);
                password.setEnabled(!loading);
                useAsStorage.setEnabled(!loading);
                loadingSpinner.setVisible(loading);
                loadingLabel.setVisible(loading);
                window.pack();
            });
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}