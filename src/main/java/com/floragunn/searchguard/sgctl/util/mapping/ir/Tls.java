package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Map<String, List<String>> profileAllowedIPs = new HashMap<>(); // transport only: List of IP addresses (value) to allow for this profile (key)
    Map<String, List<String>> profileDeniedIPs = new HashMap<>(); // transport only: List of IP addresses (value) to deny for this profile (key)
    List<String> remoteClusterAllowedIPs; // List of IP addresses to allow for remote cluster
    List<String> remoteClusterDeniedIPs; // List of IP addresses to deny remote cluster

    // check if possible constraints are satisfied -> is this checking needed??
    public boolean checkConstraints(Map<String, Object> options) {

        if (options.containsKey("xpack.security.http.ssl.keystore.key_password")
            && options.containsKey("ssl.keystore.secure_password")) {
            IntermediateRepresentation.errorLog(
                    "You cannot use keystore.key_password and keystore.secure_password at the same time",
                    2
            );
            return false;
        }

        return true;

    }

    // check an input option against all possible options acc. to the xpack docs
    public void handleTlsOptions(String optionName, Object optionValue) {
        boolean error = false;

        // Booleans
        if (IntermediateRepresentation.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    enabled = value; break;
                default:
                    error = true;
            }
        }

        else if (IntermediateRepresentation.assertType(optionValue, String.class)) {
            String value = (String) optionValue;
            switch (optionName) {
                case "keystore.path":
                    keystorePath = value; break;

                case "keystore.password":
                    keystorePassword = value; break;

                case "keystore.secure_password": // can not be migrated since it is not visible
                    IntermediateRepresentation.errorLog(optionName + " is not visible", 1);
                    break;

                case "keystore.key_password":
                    keystoreKeyPassword = value; break;

                case "keystore.secure_key_password":
                    IntermediateRepresentation.errorLog(optionName + " is not visible", 1);
                    break;

                case "truststore.path":
                    truststorePath = value; break;

                case "truststore.password":
                    truststorePassword = value; break;

                case "truststore.secure_password":
                    IntermediateRepresentation.errorLog(optionName + " is not visible", 1);
                    break;

                case "keystore.type":
                    if (value.equals("jks") || value.equals("PKCS12")) {
                        keystoreType = value;
                    } else {
                        IntermediateRepresentation.errorLog(optionName + " with type " + value + " is not known", 2);
                        error = true;
                    }
                    break;

                case "truststore.type":
                    if (value.equals("PKCS12")) {
                        truststoreType = value;
                    } else {
                        IntermediateRepresentation.errorLog(optionName + " with type " + value + " is not known", 2);
                        error = true;
                    }
                    break;

                case "certificate":
                    certificatePath = value; break;

                case "key":
                    privateKeyPath = value; break;

                case "key_passphrase":
                    privateKeyPassword = value; break;

                case "secure_key_passphrase":
                    IntermediateRepresentation.errorLog(optionName + " is not visible", 1);
                    break;

                case "verification_mode":
                    if (value.equals("full") || value.equals("certificate") || value.equals("none")) {
                        verificationMode = value;
                    } else {
                        error = true;
                    }
                    break;

                case "client_authentication":
                    if (value.equals("required") || value.equals("optional") ||  value.equals("none")) {
                        clientAuthMode = value;
                    } else {
                        error = true;
                    }
                    break;

                default:
                    error = true;
            }
        }

        else if (IntermediateRepresentation.assertType(optionValue, List.class)) {
            List<?> value = (List<?>) optionValue;

            if (value.isEmpty()) {
                return;
            }

            // Regex for profileAllowedIPs/profileDeniedIPs with optionName profiles.$PROFILE.xpack.security.filter. allow or deny
            Pattern profileIPsPattern = Pattern.compile("^profiles\\.([^.]+)\\.xpack\\.security\\.filter\\.(allow|deny)$");
            Matcher mProfileIPs = profileIPsPattern.matcher(optionName);

            if (!(value.get(0) instanceof String)) {
                error = true;
            } else if(mProfileIPs.find()) {
                String profile = mProfileIPs.group(1);
                String action  = mProfileIPs.group(2);

                if ("allow".equals(action)) {
                    profileAllowedIPs
                        .computeIfAbsent(profile, k -> new ArrayList<>())
                        .addAll((List<String>) value);

                } else if ("deny".equals(action)) {
                    profileDeniedIPs
                        .computeIfAbsent(profile, k -> new ArrayList<>())
                        .addAll((List<String>) value);
                }
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
                        error = true;
                }
            }
        }

        if (error) {
            System.out.println("Invalid option of type " + optionValue.getClass() + ": " + optionName + " = " + optionValue);
        }
    }
}
