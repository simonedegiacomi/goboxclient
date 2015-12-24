package com.goboxstorage;

import mydb.MyDB;

public class Main {

    public static void main(String[] args) {
        try {
            // Connect to the database
            MyDB db = new MyDB("files.db");

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        // Caricare configurazione
        // Avviare server locale configurazione
        // Connetersi server
        // Avviare ilistener ws
    }
}
