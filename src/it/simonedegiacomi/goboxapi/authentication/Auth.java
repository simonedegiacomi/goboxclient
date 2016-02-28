package it.simonedegiacomi.goboxapi.authentication;

import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.EasyHttpsException;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;

/**
 * The object of this class contains the credentials of a GoBoxAccount.
 * To use any of the API of this package you need this object. Auth also
 * provides the necessary methods to talk with the server to check the data.
 *
 * Created on 31/12/15.
 * @author Degiacomi Simone
 */
public class Auth {

    /**
     * Type of session: client or storage mode
     */
    public enum Modality { CLIENT, STORAGE }

    /**
     * The current mode
     */
    private Modality mode;

    private String username;

    /**
     * URLs of the environment. This is transient because it shouldn't be serialized
     */
    private transient static URLBuilder urls;

    /**
     * Token to use to authenticate with the server
     */
    private String token;

    public Auth () { }

    /**
     * Let you to set the urls
     * @param builder Urls of the environment
     */
    public static void setUrlBuilder (URLBuilder builder) {
        urls = builder;
    }

    /**
     * Try to login with the information set. This method will block the thread until
     * the login is complete.
     * @return true if the user is logged, false if the credentials aren't valid
     * @throws AuthException Exception thrown if there is some network or strange error
     * but not when the credentials aren't valid
     */
    public boolean login (String password) throws AuthException {
        try {
            // Get the json of the authentication
            JsonObject authJson = new JsonObject();
            authJson.addProperty("username", username);
            authJson.addProperty("password", password);
            authJson.addProperty("type", mode == Modality.CLIENT ? 'C' : 'S');

            // Make the https request
            JsonObject response = (JsonObject) EasyHttps.post(urls.get("login"), authJson, null);
            // evaluate the response
            String result = response.get("result").getAsString();
            if (result.equals("logged in")) {
                token = response.get("token").getAsString();
                return true;
            }
            return false;
        } catch (EasyHttpsException ex) {
            if(ex.getResponseCode() == 401)
                return false;
            throw new AuthException(ex.toString());
        } catch (IOException ex) {
            throw new AuthException(ex.toString());
        }
    }

    /**
     * Check if the token of the object is valid. This method block the thread until
     * the response from the server is retrieved.
     * @return true if the token is valid, false otherwise
     * @throws AuthException Network errors
     */
    public boolean check() throws AuthException {
        if (token == null)
            throw new AuthException("Token is null");
        try {
            JsonObject response = (JsonObject) EasyHttps.post(urls.get("authCheck"), null, token);
            if(!response.get("state").getAsString().equals("valid"))
                return false;
            token = response.get("newOne").getAsString();
            return true;
        } catch (EasyHttpsException ex) {
            if(ex.getResponseCode() == 401)
                return false;
            throw new AuthException("Check failed");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new AuthException("Check failed");
        }
    }

    public Modality getMode() {
        return mode;
    }

    public void setMode(Modality mode) {
        this.mode = mode;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Authorize an http connection made to the server
     * @param conn Connection to authorize
     */
    public void authorize (HttpsURLConnection conn) {
        conn.setRequestProperty("Authorization", getHeaderToken());
    }

    /**
     * Authorize the websocket connection
     * @param server Websocket to authorize
     */
    public void authorizeWs(MyWSClient server) {
        server.addHttpHeader("Authorization", getHeaderToken());
    }

    /**
     * Return the value of the 'Authorization' http header
     * @return Value of the authorization http header
     */
    private String getHeaderToken () {
        return "Bearer " + token;
    }
}