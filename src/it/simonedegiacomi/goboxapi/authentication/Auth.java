package it.simonedegiacomi.goboxapi.authentication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The object of this class contains the credentials
 * of a GoBoxAccount. To use any of the API of this
 * package you need this object. Auth also provides
 * the necessary methods to talk with the server
 * to check the data
 *
 * Created by Degiacomi Simone on 31/12/15.
 */
public class Auth {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(Auth.class.getName());

    /**
     * Type of session, client mode
     */
    public enum Modality { CLIENT_MODE, STORAGE_MODE };

    private Modality mode;

    private String username;

    /**
     * URLs of the gobox server. This is transient because it shouldn't be serialized
     */
    private transient static final URLBuilder urls = Config.getInstance().getUrls();

    /**
     * Password of the user. This field is not-null only on
     * the login phase
     */
    private String password;

    /**
     * Token to use to authenticate with the server
     */
    private String token;

    public Auth () { }

    public Auth(String username) {
        this.username = username;
    }

    /**
     * Try to login with the information set. This method
     * will block the thread until the login is complete
     * @throws AuthException
     */
    public void login() throws AuthException {
        try {
            // Get the json of the authentication
            JsonElement authJson = new Gson().toJsonTree(this, Auth.class);

            // remove the password
            this.password = null;
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
            token = response.get("newOne").getAsString();
            return true;
        } catch (Exception ex) {
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

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Authorize an http connection made to the server
     * @param conn Connection to authorize
     */
    public void authorize (HttpsURLConnection conn) {
        conn.setRequestProperty("Authorization", "Bearer " + token);
    }
}
