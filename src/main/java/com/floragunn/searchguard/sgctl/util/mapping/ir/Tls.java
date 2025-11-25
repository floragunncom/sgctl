package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.List;

public class Tls {
    boolean enabled; // activation of TLS

    // Keystore
    String keystorePath; // contains certificate and private key
    String keystoreType; // format of the keystore
    String keystorePassword; // pw of keystore -> NO PLAINTEXT!

    // Truststore
    String truststorePath;
    String truststoreType;
    String truststorePassword;

    // PEM -> can be used instead of keystores
    String certificatePath;
    String privateKeyPath;
    List<String> certificateAuthorities = new ArrayList<>(); // paths

    // TLS
    String verificationMode; // full, certificate, or none
    boolean clientAuth; // whether clients must present a client certificate
    boolean hostnameVerificationEnabled; // whether hostname in certificates must match the node name

    // Constraints
    List<String> supportedProtocols = new ArrayList<>();
    List<String> ciphers = new ArrayList<>();

    public boolean assertType(Object object, Class<?> type) {
        return type.isInstance(object);
    }

    public void handleTlsOptions(String optionName, Object optionValue) {
        switch (optionName) { // all option names acc. to the xpack documentation, not all may be needed
            case "enabled":  break;
            case "supported_protocols": break;
            case "client_authentication": break;
            case "verification_mode": break;
            case "cipher_suites": break;
            case "key": break;
            case "key_passphrase": break;
            case "secure_key_passphrase": break;
            case "certificate": break;
            case "certificate_authorities": break;
            case "keystore.path": break;
            case "keystore.password": break;
            case "keystore.secure_password": break;
            case "keystore.key_password": break;
            case "keystore.secure_key_password": break;
            case "truststore.path": break;
            case "truststore.password": break;
            case "truststore.secure_password": break;
            case "keystore.type": break;
            case "truststore.type": break;
        }
    }
}
