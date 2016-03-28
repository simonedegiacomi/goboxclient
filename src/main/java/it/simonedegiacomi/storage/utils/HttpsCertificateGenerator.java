package it.simonedegiacomi.storage.utils;

import com.sun.net.httpserver.HttpsConfigurator;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

/**
 * Created by simone on 19/03/16.
 */
public class HttpsCertificateGenerator {

    private static final int KEY_SIZE = 1024;

    private char[] password;

    private KeyStore keyStore;

    private SSLContext sslContext;

    private CertAndKeyGen keyPair;

    public HttpsCertificateGenerator () throws KeyStoreException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException, KeyManagementException, IOException, UnrecoverableKeyException {

        generatePassword();

        // Create a new keystore
        keyStore = KeyStore.getInstance("JKS");

        // Initialize the key store, The two null parameters are the input stream and the password used
        // to recover a saved keystore (meaningless in our case)
        keyStore.load(null, null);

        // Prepare the pair of key selecting the type of algorithm
        keyPair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        X500Name x500Name = new X500Name("address", "GoBox", "GoBox", "city", "state", "country");

        // Generate the key
        keyPair.generate(KEY_SIZE);

        // Get the  generated private key
        PrivateKey privateKey = keyPair.getPrivateKey();

        X509Certificate chain = keyPair.getSelfCertificate(x500Name, new Date(), (long) 1096 * 24 * 60 * 60);

        // Save the generated key
        keyStore.setKeyEntry("GoBox Direct", privateKey, password, new X509Certificate[] { chain });

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

    private void generatePassword () {
        password = new char[255];
        Random r = new Random();
        for(int i = 0;i < password.length; i++)
            password[i] = (char) (r.nextInt(26) + 'a');
    }

    public PrivateKey getPrivateKey () {
        return keyPair.getPrivateKey();
    }

    public PublicKey getPublicKey () {
        return keyPair.getPublicKey();
    }

    public SSLContext getSSLContext () {
        return sslContext;
    }

    public HttpsConfigurator getHttpsConfigurator() {
        return new HttpsConfigurator(sslContext);
    }
}