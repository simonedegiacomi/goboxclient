package com.goboxstorage;

import goboxclient.Config;
import goboxclient.ConfigTool;
import mydb.*;
import webstorage.WebStorage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.out.println("GoBoxStorage");

        // Connect to the database
        try {
            Config config = loadConfig();
            log.fine("Configuration loaded");
            MyDB db = new MyDB("files.db");
            WebStorage storage = new WebStorage(config, db);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
        }
    }

    private static Config loadConfig () {
        try {
            // Try to load the config
            return Config.load();
        } catch (Exception ex) {
            log.fine("No config file found");
            // If something fails, it means that there is no config
            // file, so let's create a new config

            Config newConfig = new Config();
            newConfig.setProperty("SERVER_WS", "ws://goboxserver-simonedegiacomi.c9users.io/api/ws/storage");
            newConfig.setProperty("SERVER_API", "https://goboxserver-simonedegiacomi.c9users.io/api");
            newConfig.setProperty("SERVER_LOGIN", "https://goboxserver-simonedegiacomi.c9users.io/api/user/login");
            newConfig.setProperty("SERVER_CHECK", "https://goboxserver-simonedegiacomi.c9users.io/api/user/check");

            // First ask the user credentials and where store the files
            log.info("Stating configuration tool");
            ConfigTool tool = new ConfigTool(newConfig);
            // Now try to connect
            try {
                WebStorage.login(newConfig, tool.getPassword());
                log.info("Credentials verified");
                newConfig.save();
                log.fine("New configuration created");
                return newConfig;
            } catch (Exception e) {
                log.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        }
    }
}