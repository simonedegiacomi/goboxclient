package com.goboxstorage;

import goboxstorage.Config;
import goboxstorage.ConfigTool;
import mydb.MyDB;
import webstorage.WebStorage;

public class Main {

    public static void main(String[] args) {
        System.out.println("GoBoxStorage");
        Config config = loadConfig();

        // Connect to the database
        try {
            MyDB db = new MyDB("files.db");
            WebStorage storage = new WebStorage(config, db);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static Config loadConfig () {
        try {
            // Try to load the config
            return Config.load();
        } catch (Exception ex) {
            // If something fails, it means that there is no config
            // file, so let's create a new config

            Config newConfig = new Config();
            newConfig.setProperty("SERVER_WS", "ws://goboxserver-simonedegiacomi.c9users.io/api/ws/storage");
            newConfig.setProperty("SERVER_API", "https://goboxserver-simonedegiacomi.c9users.io/api");
            newConfig.setProperty("SERVER_LOGIN", "https://goboxserver-simonedegiacomi.c9users.io/api/user/login");
            newConfig.setProperty("SERVER_CHECK", "https://goboxserver-simonedegiacomi.c9users.io/api/user/check");

            System.out.println("No configuration found.\n");
            // First ask the user credentials and where store the files
            ConfigTool tool = new ConfigTool(newConfig);
            // Now try to connect
            try {
                WebStorage.login(newConfig, tool.getPassword());
                System.out.println(newConfig.getProperty("token"));
                newConfig.save();
                return newConfig;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}