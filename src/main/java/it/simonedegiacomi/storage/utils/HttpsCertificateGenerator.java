package it.simonedegiacomi.storage.utils;

import com.sun.net.httpserver.HttpsConfigurator;
import it.simonedegiacomi.configuration.Config;
import org.apache.log4j.Logger;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

/**
 * Created on 19/03/16.
 * @author Degiacomi Simone
 */
public class HttpsCertificateGenerator {

    private static final String GOBOX_ALIAS = "GoBoxDirect";

    private final Logger logger = Logger.getLogger(HttpsCertificateGenerator.class);

    private final Config config = Config.getInstance();

    private final File KEYSTORE_FILE = new File(config.getProperty("keyStoreFile", "config/keystore.ks"));

    private static final int KEY_SIZE = 1024;

    private char[] password;

    private KeyStore keyStore;

    private X509Certificate certificate;

    private SSLContext sslContext;

    private CertAndKeyGen keyPair;

    public HttpsCertificateGenerator () throws KeyStoreException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException, KeyManagementException, IOException, UnrecoverableKeyException {

        // Reload or create the password
        initPassword();

        // Reload or init keystore
        initKeystore();

        if (!keyStore.containsAlias(GOBOX_ALIAS)) {

            // Prepare the pair of key selecting the type of algorithm
            keyPair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
            X500Name x500Name = new X500Name("address", "GoBox", "GoBox", "city", "state", "country");

            // Generate the key
            keyPair.generate(KEY_SIZE);

            // Get the  generated private key
            PrivateKey privateKey = keyPair.getPrivateKey();

            certificate = keyPair.getSelfCertificate(x500Name, new Date(), (long) 1096 * 24 * 60 * 60);

            // Save the generated key
            keyStore.setKeyEntry(GOBOX_ALIAS, privateKey, password, new X509Certificate[]{certificate});

            try {
                keyStore.store(new FileOutputStream(KEYSTORE_FILE), password);
            } catch (IOException ex) {
                logger.warn("Cannot save keystore");
            }
        } else {
            certificate = (X509Certificate) keyStore.getCertificate(GOBOX_ALIAS);
        }

        // Create the ssl context
        sslContext = SSLContext.getInstance("TLS");

        // Get the key manager factory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        // and initialize it with the keystore and the same password
        keyManagerFactory.init(keyStore, password);

        // Get the trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        // and initialize it with the keystore
        trustManagerFactory.init(keyStore);

        // Initialize the ssl context with the used key manager and the used trust manager. The last null
        // parameters is the source used for random codes
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
    }

    /**
     * Create or reload the password for the keystore
     */
    private void initPassword() {

        // Check in the config
        if (config.hasProperty("keyStorePassword")) {
            password = config.getProperty("keyStorePassword", String.valueOf(new Random().nextDouble())).toCharArray();
            return;
        }

        // Create a new one
        password = new char[255];
        Random r = new Random();
        for(int i = 0;i < password.length; i++)
            password[i] = (char) (r.nextInt(26) + 'a');

        // Save the used password in the config
        config.setProperty("keyStorePassword", new String(password));
    }

    /**
     * Reload or init a new keystore
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private void initKeystore () throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {

        // Create a new keystore
        keyStore = KeyStore.getInstance("JKS");

        if (new File(config.getProperty("keyStoreFile", "keystore.ks")).exists()) {
            try (FileInputStream in = new FileInputStream(KEYSTORE_FILE)) {
                keyStore.load(in, password);
            } catch (IOException e) {
                logger.info("No keystore found. Creating a new one");
                keyStore.load(null, null);
            }
            return;
        }

        // Fallback with a new keystore
        keyStore.load(null, null);
    }

    public X509Certificate getCertificate () {
        return certificate;
    }

    public SSLContext getSSLContext () {
        return sslContext;
    }

    public HttpsConfigurator getHttpsConfigurator() {
        return new HttpsConfigurator(sslContext);
    }

}