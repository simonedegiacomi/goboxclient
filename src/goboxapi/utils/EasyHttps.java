package goboxapi.utils;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Created by Degiacomi Simone on 27/12/2015.
 */
public class EasyHttps {
    public static JSONObject post(String url, JSONObject data, String authorization) throws IOException, EasyHttpsException, JSONException {
        // Open the connection
        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
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
        // Open the reader
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        // Read all the response
        StringBuilder builder = new StringBuilder();
        String temp;
        while((temp = in.readLine()) != null)
            builder.append(temp);
        // Close the reader and the request
        in.close();
        conn.disconnect();
        // Parse the response
        return new JSONObject(builder.toString());
    }
}
