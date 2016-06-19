package it.simonedegiacomi.utils;

import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import org.apache.log4j.Logger;

/**
 * This class expones a method used to set the
 * proxy configuration
 * Created on 22/11/2015.
 * @author Degiacomi Simone
 */
public class EasyProxy {

    private static final Logger log = Logger.getLogger(EasyProxy.class);

    /**
     * Apply the proxy configuration from the Config class.
     **/
    public static void handleProxy(final Config config) {

        // Check if the proxy is set
        if(!Boolean.parseBoolean(config.getProperty("useProxy", "false")))
            return ;

        // Set the proxy address and port
        String proxyIP = config.getProperty("proxyIP", "127.0.0.1");
        String proxyPort = config.getProperty("proxyPort", "3128");
        System.setProperty("http.proxyHost", proxyIP);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("https.proxyHost", proxyIP);
        System.setProperty("https.proxyPort", proxyPort);
        MyWSClient.setProxy(proxyIP, Integer.parseInt(proxyPort));

        log.info("Proxy: " + proxyIP+ ':' + proxyPort);
    }
}