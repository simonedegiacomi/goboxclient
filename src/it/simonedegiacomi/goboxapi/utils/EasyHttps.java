package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Tool Class used to make simply https request
 *
 * Created by Degiacomi Simone onEvent 27/12/2015.
 */
public class EasyHttps {

    private static final Logger log = Logger.getLogger(EasyHttps.class.getName());

    private final static JsonParser parser = new JsonParser();

    /**
     * Make a new HTTPS request
     * @param url URL
     * @param data Data to send as POST
     * @param authorization Authorization header
     * @return Response of the request
     * @throws IOException
     * @throws EasyHttpsException
     */
    public static JsonElement post(URL url, JsonElement data, String authorization) throws IOException, EasyHttpsException {

        // Open the connection
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        // Set the authorization
        if (authorization != null)
            conn.setRequestProperty("Authorization", "Bearer " + authorization);
        // Set the method
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        // If the data is specified, sendEvent it
        if (data != null) {
            // Set the output true
            conn.setDoOutput(true);
            // Get the string of the json
            String jsonString = data.toString();
            // Set the length
            conn.setRequestProperty("Content-Length", String.valueOf(jsonString.length()));
            // Get the output stream
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            // Write the json
            out.print(jsonString);
            // Close the output
            out.close();
        }
        // Read the response code
        int response = conn.getResponseCode();
        // If is not 200 throw a new exception
        if (response != 200)
            throw new EasyHttpsException(response);

        // Parse the response
        JsonElement jsonResponse = parser.parse(new InputStreamReader(conn.getInputStream()));
        conn.disconnect();

        return jsonResponse;
    }

    /**
     * Make a new HTTPS request
     * @param url URL
     * @param data Data to send as POST
     * @param authorization Authorization header
     * @return Response of the request
     * @throws IOException
     * @throws EasyHttpsException
     */
    public static JsonElement post (String url, JsonElement data, String authorization) throws IOException, EasyHttpsException {
        return post(new URL(url), data, authorization);
    }
}
