package it.simonedegiacomi.configuration;

/**
 * Created by Simone onEvent 30/12/2015.
 */
public class ConfigurationException extends Exception {
    private String error;

    public ConfigurationException(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ConfigurationException{" +
                "error='" + error + '\'' +
                '}';
    }
}
