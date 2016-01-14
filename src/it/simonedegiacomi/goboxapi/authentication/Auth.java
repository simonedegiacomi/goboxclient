package it.simonedegiacomi.goboxapi.authentication;

import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import org.json.JSONException;
import org.json.JSONObject;

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

    public static final int CLIENT = 0;
    /**
     * Type of session used to authenticate the client
     * as it.simonedegiacomi.storage
     */
    public static final int STORAGE = 1;

    private final String username;
    private char[] password;
    private String token;
    private int mode;
    private int id;

    public Auth(String username) {
        this.username = username;
    }

    /**
     * Try to login with the information setted. This method
     * will block the thread until the login is complete
     * @throws AuthException
     */
    public void login() throws AuthException {
        try {
            // Get the json of the authentication
            JSONObject authJson = this.toJSON();
            // Make the https request
            JSONObject response = EasyHttps.post("https://goboxserver-simonedegiacomi.c9users.io/api/user/login", authJson, null);
            // evalutate the response
            String result = response.getString("result");
            switch (result) {
                case "Storage already registered":
                    throw new AuthException(result);
                case "logged in":
                    // Save the token
                    token = response.getString("token");
                    // remove the password
                    for(int i = 0;i < password.length; i++)
                        password[i] = 0;
                    this.password = null;
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
     * server is retrived.
     * @throws AuthException
     */
    public boolean check() throws AuthException {
        if (token == null)
            throw new AuthException("Token is null");
        try {
            JSONObject response = EasyHttps.post("https://goboxserver-simonedegiacomi.c9users.io/api/user/check", null, token);
            token = response.getString("newOne");
            return true;
        } catch (JSONException ex) {
            throw new AuthException("Invalid token");
        } catch (Exception ex) {
            throw new AuthException("Check failed");
        }
    }

    /**
     * Return the representation of the instance of this object in
     * JSON (JSONObject)
     * @return JSON of this object
     */
    public JSONObject toJSON () {
        JSONObject authJson = new JSONObject();
        try {
            authJson.put("username", username);
            if(password != null)
                authJson.put("password", new String(password));
            authJson.put("type", (mode == CLIENT ? "C" : "S"));
            authJson.put("token", token);
        } catch (JSONException ex) { }
        return  authJson;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
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
