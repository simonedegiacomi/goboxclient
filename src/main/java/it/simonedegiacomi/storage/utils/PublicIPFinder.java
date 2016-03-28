package it.simonedegiacomi.storage.utils;

import org.apache.tika.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Created by simone on 19/03/16.
 */
public class PublicIPFinder {

    public static final String DEFAULT_WEB_SERVICE_URL = "http://checkip.amazonaws.com/";

    public static String find () throws IOException {
        return IOUtils.toString(new URL(DEFAULT_WEB_SERVICE_URL).openStream());
    }

    public static String find (String webServiceUrl) {
        return find(DEFAULT_WEB_SERVICE_URL);
    }
}