package org.tvrenamer.model;

import org.tvrenamer.model.util.CryptographyUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

public class ProxySettings {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(ProxySettings.class.getName());

    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private boolean enabled = false;
    private String hostname;
    private String port;
    private boolean authenticationRequired;
    private String username;
    private String encryptedPassword;

    public ProxySettings() {
        enabled = false;
        hostname = "";
        port = "";
        username = "";
        encryptedPassword = "";
    }

    /**
     * If enabled, set the global system proxy settings so all subsequent
     * connections use this.
     */
    public void apply() {
        if (enabled) {
            System.setProperty(HTTP_PROXY_HOST, hostname);
            System.setProperty(HTTP_PROXY_PORT, port);
            setupAuthenticator();
        } else {
            System.setProperty(HTTP_PROXY_HOST, "");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String proxyUsername) {
        this.username = proxyUsername;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getDecryptedPassword() {
        String decrypted = CryptographyUtils.decrypt(encryptedPassword);
        // logger.fine("Decrypting [" + encryptedPassword + "] into [" + decrypted + "]"); // Disable logging of
                                                                       // sensitive information, but helps debug
        return decrypted;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
        setupAuthenticator();
    }

    public void setPlainTextPassword(String passwordToEncyrypt) {
        String encrypted = CryptographyUtils.encrypt(passwordToEncyrypt);
        // logger.fine("Encrypting [" + passwordToEncyrypt + "] into [" + encrypted + "]"); // Disable logging of
                                                                        // sensitive information, but helps debug
        this.encryptedPassword = encrypted;
        setupAuthenticator();
    }

    private void setupAuthenticator() {
        if (authenticationRequired) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String decrypted = CryptographyUtils.decrypt(encryptedPassword);
                    if (decrypted == null) {
                        // TODO: throw exception?
                        return null;
                    }
                    return new PasswordAuthentication(username.replace("\\", "\\\\"),
                                                      decrypted.toCharArray());
                }
            });
        }
    }

    @Override
    public String toString() {
        return "[enabled=" + enabled + ", hostname=" + hostname + ", port=" + port + ", username=" + username + "]";
    }

    @Override
    public int hashCode() {
        return 3852104;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        ProxySettings rhs = (ProxySettings) obj;

        if (this.enabled == rhs.enabled && this.hostname.equals(rhs.hostname) && this.port.equals(rhs.port)
            && this.authenticationRequired == rhs.authenticationRequired && this.username.equals(rhs.username)
            && this.encryptedPassword.equals(rhs.encryptedPassword))
        {
            return true;
        }
        return false;
    }
}
