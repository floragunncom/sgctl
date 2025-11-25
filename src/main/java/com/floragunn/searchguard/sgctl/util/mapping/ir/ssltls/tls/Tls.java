package com.floragunn.searchguard.sgctl.util.mapping.ir.ssltls.tls;

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
}
