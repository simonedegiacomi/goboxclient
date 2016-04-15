package it.simonedegiacomi.storage.utils;

import org.apache.tika.io.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Util to find the public ip and the probable local ip
 * Created on 19/03/16.
 * @author Degiacomi Simone
 */
public class IPFinder {

    /**
     * Default service url used to find the public ip
     */
    public static final String DEFAULT_WEB_SERVICE_URL = "http://checkip.amazonaws.com/";

    /**
     * Return the public ip address
     * @return Ip address
     * @throws IOException
     */
    public static String findPublic () throws IOException {
        return IOUtils.toString(new URL(DEFAULT_WEB_SERVICE_URL).openStream()).replace("\n", "");
    }

    /**
     * Return the <em>probable</em> local ip address.
     * This method get the host address of the first network
     * @return Probable local ip address
     * @throws UnknownHostException
     */
    public static String findProbableLocal () throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }
}