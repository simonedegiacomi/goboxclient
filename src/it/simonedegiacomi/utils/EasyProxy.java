package it.simonedegiacomi.utils;

import com.sun.org.apache.xpath.internal.operations.Bool;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * This class expones a method used to set the
 * proxy configuration
 * Created by Degiacomi Simone on 22/11/2015.
 */
public class EasyProxy {
    /**
     * Apply the proxy configurationfrom the Config class.
     **/
    public static void manageProxy(Config config) {

        // Check if the proxy is setted
        if(!Boolean.parseBoolean(config.getProperty("useProxy")))
            return ;

        // Set the proxy address and port
        System.setProperty("http.proxyHost", config.getProperty("proxyIP"));
        System.setProperty("http.proxyPort", config.getProperty("proxyPort"));
        System.setProperty("https.proxyHost", config.getProperty("proxyIP"));
        System.setProperty("https.proxyPort", config.getProperty("proxyPort"));
        MyWSClient.setProxy(config.getProperty("proxyIP"), config.getProperty("proxyPort"));

        System.out.printf("Proxy set (%s:%s)\n", config.getProperty("proxyIP"), config.getProperty("proxyPort"));

        // Check if the proxy use an authentication
        if(Boolean.parseBoolean(config.getProperty("authenticateProxy")))
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(config.getProperty("proxyUser"), config.getProperty("proxyPassword").toCharArray());
                        }
                    }
            );
    }
}