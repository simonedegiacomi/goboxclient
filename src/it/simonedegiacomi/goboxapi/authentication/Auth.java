package it.simonedegiacomi.goboxapi.authentication;

import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.EasyHttpsException;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The object of this class contains the credentials
 * of a GoBoxAccount. To use any of the API of this
 * package you need this object. Auth also provides
 * the necessary methods to talk with the server
 * to check the data
 *
 * Created by Degiacomi Simone onEvent 31/12/15.
 */
public class Auth {

    private static final Logger log = Logger.getLogger(Auth.class.getName());

    /**
     * Type of session, client mode
     */
    public enum Modality { CLIENT, STORAGE }

    private Modality mode;

    private String username;

    /**
     * URLs of the gobox server. This is transient because it shouldn't be serialized
     */
    private transient static final URLBuilder urls = new URLBuilder();

    /**
     * Token to use to authenticate with the server
     */
    private String token;

    public Auth () {
        try {
            urls.load();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError("Cannot load GoBox Server urls");
        }
    }

    /**
     * Try to login with the information set. This method
     * will block the thread until the login is complete
     * @throws AuthException
     */
    public void login(String password) throws AuthException {
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
            switch (result) {
                case "Storage already registered":
                    throw new AuthException(result);
                case "logged in":
                    // Save the token
                    token = response.get("token").getAsString();
                    break;
                default:
                    throw new AuthException("Login failed");
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.toString(), ex);
            throw new AuthException("authentication failed");
        }
    }

    /**
     * Check if the token of the object is valid. This
     * method block the thread until the response from the
     * server is retrieved.
     * @throws AuthException
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

    public void authorizeWs(MyWSClient server) {
        server.addHttpHeader("Authorization", getHeaderToken());
    }

    private String getHeaderToken () {
        return "Bearer " + token;
    }
}
