package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents TLS/SSL configuration options as read from {@code elasticsearch.yml}.
 * <p>
 * This class collects all TLS-related settings (keystore, truststore, PEM, protocols,
 * ciphers and IP filters) and provides a method {@link #handleTlsOptions(String, Object, String, File)}
 * to map flat option names to the corresponding fields.
 */
public class Tls {

    /** Activation flag for TLS. */
    private boolean enabled;

    // Keystore
    /** Path to the keystore containing certificate and private key. */
    private String keystorePath;
    /** Type/format of the keystore (e.g. jks, PKCS12). */
    private String keystoreType;
    /** Password of the keystore (plain value – secure_* variants are ignored). */
    private String keystorePassword;
    /** Password of the key inside the keystore. */
    private String keystoreKeyPassword;

    // Truststore
    /** Path to the truststore containing trusted CAs. */
    private String truststorePath;
    /** Type/format of the truststore (e.g. PKCS12). */
    private String truststoreType;
    /** Password of the truststore. */
    private String truststorePassword;

    // PEM -> can be used instead of keystores
    /** Path to the certificate in PEM format. */
    private String certificatePath;
    /** Path to the private key in PEM format. */
    private String privateKeyPath;
    /** Optional password for the private key. */
    private String privateKeyPassword;
    /** List of certificate authority file paths. */
    private List<String> certificateAuthorities = List.of();

    // TLS modes
    /**
     * Verification mode defining how hostnames are validated in certificates.
     * Valid values: {@code full}, {@code certificate}, {@code none}.
     */
    private String verificationMode;
    /**
     * Client authentication mode defining if client certificates are required.
     * Valid values: {@code required}, {@code optional}, {@code none}.
     */
    private String clientAuthMode;

    // Constraints
    /** Supported protocol versions (e.g. TLSv1.2, TLSv1.3). */
    private List<String> supportedProtocols = List.of();
    /** List of cipher suites. */
    private List<String> ciphers = List.of();

    // IP filtering (https://www.elastic.co/docs/reference/elasticsearch/configuration-reference/security-settings#ip-filtering-settings)
    /** List of allowed IP addresses for this TLS context. */
    private List<String> allowedIPs = List.of();
    /** List of denied IP addresses for this TLS context. */
    private List<String> deniedIPs = List.of();
    /** List of allowed IP addresses for remote clusters. */
    private List<String> remoteClusterAllowedIPs = List.of();
    /** List of denied IP addresses for remote clusters. */
    private List<String> remoteClusterDeniedIPs = List.of();

    private static final String THIS_FILE = "elasticsearch.yml";

    // region Getters

    /**
     * @return list of supported protocol versions.
     */
    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    /**
     * @return list of configured cipher suites.
     */
    public List<String> getCiphers() {
        return ciphers;
    }

    /**
     * @return list of allowed IP addresses for this TLS context, or {@code null} if none set.
     */
    public List<String> getAllowedIPs() {
        return allowedIPs;
    }

    /**
     * @return list of denied IP addresses for this TLS context, or {@code null} if none set.
     */
    public List<String> getDeniedIPs() {
        return deniedIPs;
    }

    /**
     * @return list of allowed IP addresses for remote clusters, or {@code null} if none set.
     */
    public List<String> getRemoteClusterAllowedIPs() {
        return remoteClusterAllowedIPs;
    }

    /**
     * @return list of denied IP addresses for remote clusters, or {@code null} if none set.
     */
    public List<String> getRemoteClusterDeniedIPs() {
        return remoteClusterDeniedIPs;
    }

    /**
     * @return {@code true} if TLS is enabled.
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @return path to the keystore, or {@code null} if not configured.
     */
    public String getKeystorePath() {
        return keystorePath;
    }

    /**
     * @return keystore type (e.g. {@code jks}, {@code PKCS12}), or {@code null}.
     */
    public String getKeystoreType() {
        return keystoreType;
    }

    /**
     * @return keystore password (plain value), or {@code null}.
     */
    public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * @return key password inside the keystore, or {@code null}.
     */
    public String getKeystoreKeyPassword() {
        return keystoreKeyPassword;
    }

    /**
     * @return path to the truststore, or {@code null}.
     */
    public String getTruststorePath() {
        return truststorePath;
    }

    /**
     * @return truststore type (e.g. {@code PKCS12}), or {@code null}.
     */
    public String getTruststoreType() {
        return truststoreType;
    }

    /**
     * @return truststore password, or {@code null}.
     */
    public String getTruststorePassword() {
        return truststorePassword;
    }

    /**
     * @return path to the PEM certificate file, or {@code null}.
     */
    public String getCertificatePath() {
        return certificatePath;
    }

    /**
     * @return path to the PEM private key file, or {@code null}.
     */
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    /**
     * @return password for the PEM private key, or {@code null}.
     */
    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    /**
     * @return list of certificate authority paths, possibly empty but never {@code null}.
     */
    public List<String> getCertificateAuthorities() {
        return certificateAuthorities;
    }

    /**
     * @return verification mode (full, certificate, none), or {@code null}.
     */
    public String getVerificationMode() {
        return verificationMode;
    }

    /**
     * @return client authentication mode (required, optional, none), or {@code null}.
     */
    public String getClientAuthMode() {
        return clientAuthMode;
    }

    /**
     * Exposes the logical configuration file name used for reporting.
     *
     * @return {@code "elasticsearch.yml"}
     */
    public String getThisFileName() {
        return THIS_FILE;
    }

    // endregion

    /**
     * Maps a flat TLS option to its corresponding field in this {@link Tls} instance.
     * <p>
     * This method is called by the Elasticsearch YAML reader and is responsible for:
     * <ul>
     *     <li>Type checking the provided {@code optionValue}</li>
     *     <li>Assigning known options to fields</li>
     *     <li>Recording migrated/ignored/unknown keys via {@link MigrationReport}</li>
     * </ul>
     *
     * @param optionName the option name relative to the TLS prefix
     *                   (e.g. {@code "keystore.path"}, {@code "enabled"}).
     * @param optionValue the parsed value from the configuration file.
     * @param keyPrefix a prefix used for reporting (e.g. {@code "xpack.security.transport.ssl."}).
     * @param configFile the configuration file from which the option was read.
     */
    @SuppressWarnings("unchecked")
    public void handleTlsOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        boolean keyKnown = true;
        boolean keyIgnore = false; // ignore all secure_* keys since they are not even visible

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.isType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    enabled = value;
                    break;
                default:
                    keyKnown = false;
            }
        }

        // Strings
        else if (IntermediateRepresentationElasticSearchYml.isType(optionValue, String.class)) {
            String value = (String) optionValue;
            switch (optionName) {
                case "keystore.path":
                    keystorePath = value;
                    break;

                case "keystore.password":
                    keystorePassword = value;
                    break;

                case "keystore.secure_password": // can not be migrated since it is not visible
                    keyIgnore = true;
                    break;

                case "keystore.key_password":
                    keystoreKeyPassword = value;
                    break;

                case "keystore.secure_key_password":
                    keyIgnore = true;
                    break;

                case "truststore.path":
                    truststorePath = value;
                    break;

                case "truststore.password":
                    truststorePassword = value;
                    break;

                case "truststore.secure_password":
                    keyIgnore = true;
                    break;

                case "keystore.type":
                    if (value.equals("jks") || value.equals("PKCS12")) {
                        keystoreType = value;
                    } else {
                        MigrationReport.shared.addWarning(
                                THIS_FILE,
                                keyPrefix + optionName,
                                value + " is unknown keystore type, only jks or PKCS12 are supported"
                        );
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "truststore.type":
                    if (value.equals("PKCS12")) {
                        truststoreType = value;
                    } else {
                        MigrationReport.shared.addWarning(
                                THIS_FILE,
                                keyPrefix + optionName,
                                value + " is unknown keystore type, only PKCS12 is supported"
                        );
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "certificate":
                    certificatePath = value;
                    break;

                case "key":
                    privateKeyPath = value;
                    break;

                case "key_passphrase":
                    privateKeyPassword = value;
                    break;

                case "secure_key_passphrase":
                    keyIgnore = true;
                    break;

                case "verification_mode":
                    if (value.equals("full") || value.equals("certificate") || value.equals("none")) {
                        verificationMode = value;
                    } else {
                        MigrationReport.shared.addWarning(
                                THIS_FILE,
                                keyPrefix + optionName,
                                value + " is unknown verification mode, only full, certificate or none are supported"
                        );
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                case "client_authentication":
                    if (value.equals("required") || value.equals("optional") || value.equals("none")) {
                        clientAuthMode = value;
                    } else {
                        MigrationReport.shared.addWarning(
                                THIS_FILE,
                                keyPrefix + optionName,
                                value + " is unknown client authentication mode, only required, optional or none are supported"
                        );
                        keyIgnore = true;
                        keyKnown = false;
                    }
                    break;

                default:
                    keyKnown = false;
            }
        }

        // Lists
        else if (IntermediateRepresentationElasticSearchYml.isType(optionValue, List.class)) {
            List<?> value = (List<?>) optionValue;

            if (value.isEmpty()) {
                return;
            }

            if (!(value.get(0) instanceof String)) {
                MigrationReport.shared.addManualAction(
                        THIS_FILE,
                        keyPrefix + optionName,
                        value + " is not a string but it should be"
                );
                keyIgnore = true;
                keyKnown = false;
            } else {
                switch (optionName) {
                    case "certificate_authorities":
                        certificateAuthorities = freezeStrings(value);
                        break;

                    case "supported_protocols":
                        supportedProtocols = freezeStrings(value);
                        break;

                    case "cipher_suites":
                        ciphers = freezeStrings(value);
                        break;

                    case "filter.allow":
                        allowedIPs = freezeStrings(value);
                        break;

                    case "filter.deny":
                        deniedIPs = freezeStrings(value);
                        break;

                    case "remote_cluster.filter.allow":
                        remoteClusterAllowedIPs = freezeStrings(value);
                        break;

                    case "remote_cluster.filter.deny":
                        remoteClusterDeniedIPs = freezeStrings(value);
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

    private static List<String> freezeStrings(List<?> value) {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()) {
            return List.of();
        }
        ArrayList<String> copy = new ArrayList<>(value.size());
        for (Object element : value) {
            copy.add((String) element);
        }
        return Collections.unmodifiableList(copy);
    }
}
