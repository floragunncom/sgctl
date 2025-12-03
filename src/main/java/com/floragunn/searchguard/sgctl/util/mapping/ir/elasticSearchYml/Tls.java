package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tls {
    boolean enabled; // activation of TLS

    // Keystore
    String keystorePath; // contains certificate and private key
    String keystoreType; // format of the keystore
    String keystorePassword; // pw of keystore -> NO PLAINTEXT!
    String keystoreKeyPassword; // pw of key in the keystore

    // Truststore
    String truststorePath;
    String truststoreType;
    String truststorePassword;

    // PEM -> can be used instead of keystores
    String certificatePath;
    String privateKeyPath;
    String privateKeyPassword;
    List<String> certificateAuthorities = new ArrayList<>(); // paths

    // TLS modes
    String verificationMode; // // whether hostname in certificates must match the node name: full, certificate, or none
    String clientAuthMode; // whether clients must present a client certificate: require, optional, or none

    // Constraints
    List<String> supportedProtocols = new ArrayList<>();
    List<String> ciphers = new ArrayList<>();

    // IP filtering (https://www.elastic.co/docs/reference/elasticsearch/configuration-reference/security-settings#ip-filtering-settings)
    List<String> allowedIPs; // List of IP addresses to allow
    List<String> deniedIPs; // List of IP addresses to deny
    List<String> remoteClusterAllowedIPs; // List of IP addresses to allow for remote cluster
    List<String> remoteClusterDeniedIPs; // List of IP addresses to deny remote cluster

    public List<String> getSupportedProtocols() { return supportedProtocols; }
    public List<String> getCiphers() { return ciphers; }
    public List<String> getAllowedIPs() { return allowedIPs; }
    public List<String> getDeniedIPs() { return deniedIPs; }
    public List<String> getRemoteClusterAllowedIPs() { return remoteClusterAllowedIPs; }
    public List<String> getRemoteClusterDeniedIPs() { return remoteClusterDeniedIPs; }
    public boolean getEnabled() { return enabled; }
    public String getKeystorePath() { return keystorePath; }
    public String getKeystoreType() { return keystoreType; }
    public String getKeystorePassword() { return keystorePassword; }
    public String getKeystoreKeyPassword() { return keystoreKeyPassword; }
    public String getTruststorePath() { return truststorePath; }
    public String getTruststoreType() { return truststoreType; }
    public String getTruststorePassword() { return truststorePassword; }
    public String getCertificatePath() { return certificatePath; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public String getPrivateKeyPassword() { return privateKeyPassword; }
    public List<String> getCertificateAuthorities() { return certificateAuthorities; }
    public String getVerificationMode() { return verificationMode; }
    public String getClientAuthMode() { return clientAuthMode; }


    String THIS_FILE = "elasticsearch.yml";
    // check an input option against all possible options acc. to the xpack docs
    public void handleTlsOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        boolean keyKnown = true;
        boolean keyIgnore = false; // ignore all secure_X keys since they are not even visible

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    enabled = value; break;
                default:
                    keyKnown = false;
            }
        }

        // Strings
        else if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, String.class)) {
            String value = (String) optionValue;
            switch (optionName) {
                case "keystore.path":
                    keystorePath = value; break;

                case "keystore.password":
                    keystorePassword = value; break;

                case "keystore.secure_password": // can not be migrated since it is not visible
                    keyIgnore = true;
                    break;

                case "keystore.key_password":
                    keystoreKeyPassword = value; break;

                case "keystore.secure_key_password":
                    keyIgnore = true;
                    break;

                case "truststore.path":
                    truststorePath = value; break;

                case "truststore.password":
                    truststorePassword = value; break;

                case "truststore.secure_password":
                    keyIgnore = true;
                    break;

                case "keystore.type":
                    if (value.equals("jks") || value.equals("PKCS12")) {
                        keystoreType = value;
                    } else {
                        MigrationReport.shared.addWarning(THIS_FILE, keyPrefix + optionName, value + " is unknown keystore type, only jks or PKCS12 are supported");
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "truststore.type":
                    if (value.equals("PKCS12")) {
                        truststoreType = value;
                    } else {
                        MigrationReport.shared.addWarning(THIS_FILE, keyPrefix + optionName, value + " is unknown keystore type, only PKCS12 is supported");
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "certificate":
                    certificatePath = value; break;

                case "key":
                    privateKeyPath = value; break;

                case "key_passphrase":
                    privateKeyPassword = value; break;

                case "secure_key_passphrase":
                    keyIgnore = true;
                    break;

                case "verification_mode":
                    if (value.equals("full") || value.equals("certificate") || value.equals("none")) {
                        verificationMode = value;
                    } else {
                        MigrationReport.shared.addWarning(THIS_FILE, keyPrefix + optionName, value + " is unknown verification mode, only full, certificate or none are supported");
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "client_authentication":
                    if (value.equals("required") || value.equals("optional") ||  value.equals("none")) {
                        clientAuthMode = value;
                    } else {
                        MigrationReport.shared.addWarning(THIS_FILE, keyPrefix + optionName, value + " is unknown client authentication mode, only required, optional or none are supported");
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                default:
                    keyKnown = false;
            }
        }

        else if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, List.class)) {
            List<?> value = (List<?>) optionValue;

            if (value.isEmpty()) {
                return;
            }

            if (!(value.get(0) instanceof String)) {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + optionName, value + " is not a string but it should be");
                keyIgnore = true;
                keyKnown = false;
            } else {
                switch (optionName) {
                    case "certificate_authorities":
                        certificateAuthorities = (List<String>) value;
                        break;

                    case "supported_protocols":
                        supportedProtocols = (List<String>) value;
                        break;

                    case "cipher_suites":
                        ciphers = (List<String>) value;
                        break;

                    case "filter.allow":
                        allowedIPs = (List<String>) value;
                        break;

                    case "filter.deny":
                        deniedIPs = (List<String>) value;
                        break;

                    case "remote_cluster.filter.allow":
                        remoteClusterAllowedIPs = (List<String>) value;
                        break;
                    
                    case "remote_cluster.filter.deny":
                        remoteClusterDeniedIPs = (List<String>) value;
                        break;  

                    default:
                        keyKnown = false;
                }
            }
        }

        if (keyIgnore) {
            MigrationReport.shared.addIgnoredKey(THIS_FILE, keyPrefix + optionName, configFile.getPath());
            return;
        }

        if (keyKnown) {
            MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + optionName);
        } else {
            MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + optionName, configFile.getPath());
        }
    }
}


