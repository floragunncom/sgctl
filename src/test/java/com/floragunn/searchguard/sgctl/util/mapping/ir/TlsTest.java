package com.floragunn.searchguard.sgctl.util.mapping.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.Tls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the TLS option handling for individual TLS instances.
 */
class TlsTest {

    private static final String KEY_PREFIX = "xpack.security.http.ssl.";
    private Tls tls;
    private File configFile;

    /**
     * Creates a fresh TLS instance before each test.
     */
    @BeforeEach
    void setUp() {
        tls = new Tls();
        configFile = new File("elasticsearch.yml");
    }

    /**
     * Verifies that the enabled flag is set when a boolean option is provided.
     */
    @Test
    void handleTlsOptionsShouldSetEnabledFlagForBooleanOption() {
        tls.handleTlsOptions("enabled", Boolean.TRUE, KEY_PREFIX, configFile);
        assertTrue(tls.getEnabled());
    }

    /**
     * Verifies that keystore and truststore values are assigned from string options.
     */
    @Test
    void handleTlsOptionsShouldSetKeystoreAndTruststoreValuesForStringOptions() {
        tls.handleTlsOptions("keystore.path", "keystore.p12", KEY_PREFIX, configFile);
        tls.handleTlsOptions("keystore.password", "keystorePwd", KEY_PREFIX, configFile);
        tls.handleTlsOptions("keystore.key_password", "keyPwd", KEY_PREFIX, configFile);
        tls.handleTlsOptions("truststore.path", "truststore.p12", KEY_PREFIX, configFile);
        tls.handleTlsOptions("truststore.password", "truststorePwd", KEY_PREFIX, configFile);

        assertEquals("keystore.p12", tls.getKeystorePath());
        assertEquals("keystorePwd", tls.getKeystorePassword());
        assertEquals("keyPwd", tls.getKeystoreKeyPassword());
        assertEquals("truststore.p12", tls.getTruststorePath());
        assertEquals("truststorePwd", tls.getTruststorePassword());
    }

    /**
     * Verifies that only supported keystore types are accepted.
     */
    @Test
    void handleTlsOptionsShouldAcceptOnlySupportedKeystoreTypes() {
        tls.handleTlsOptions("keystore.type", "PKCS12", KEY_PREFIX, configFile);
        assertEquals("PKCS12", tls.getKeystoreType());

        Tls otherTls = new Tls();
        otherTls.handleTlsOptions("keystore.type", "unknown-type", KEY_PREFIX, configFile);
        assertNull(otherTls.getKeystoreType());
    }

    /**
     * Verifies that only supported truststore types are accepted.
     */
    @Test
    void handleTlsOptionsShouldAcceptOnlySupportedTruststoreTypes() {
        tls.handleTlsOptions("truststore.type", "PKCS12", KEY_PREFIX, configFile);
        assertEquals("PKCS12", tls.getTruststoreType());

        Tls otherTls = new Tls();
        otherTls.handleTlsOptions("truststore.type", "jks", KEY_PREFIX, configFile);
        assertNull(otherTls.getTruststoreType());
    }

    /**
     * Verifies that certificate and key related options are mapped correctly.
     */
    @Test
    void handleTlsOptionsShouldSetCertificateAndKeyValues() {
        tls.handleTlsOptions("certificate", "cert.pem", KEY_PREFIX, configFile);
        tls.handleTlsOptions("key", "key.pem", KEY_PREFIX, configFile);
        tls.handleTlsOptions("key_passphrase", "secret", KEY_PREFIX, configFile);

        assertEquals("cert.pem", tls.getCertificatePath());
        assertEquals("key.pem", tls.getPrivateKeyPath());
        assertEquals("secret", tls.getPrivateKeyPassword());
    }

    /**
     * Verifies that valid verification modes are accepted.
     */
    @Test
    void handleTlsOptionsShouldSetVerificationModeForValidValues() {
        tls.handleTlsOptions("verification_mode", "full", KEY_PREFIX, configFile);
        assertEquals("full", tls.getVerificationMode());

        Tls otherTls = new Tls();
        otherTls.handleTlsOptions("verification_mode", "certificate", KEY_PREFIX, configFile);
        assertEquals("certificate", otherTls.getVerificationMode());

        Tls thirdTls = new Tls();
        thirdTls.handleTlsOptions("verification_mode", "none", KEY_PREFIX, configFile);
        assertEquals("none", thirdTls.getVerificationMode());
    }

    /**
     * Verifies that invalid verification modes are rejected.
     */
    @Test
    void handleTlsOptionsShouldNotSetVerificationModeForInvalidValue() {
        tls.handleTlsOptions("verification_mode", "invalid", KEY_PREFIX, configFile);
        assertNull(tls.getVerificationMode());
    }

    /**
     * Verifies that valid client authentication modes are accepted.
     */
    @Test
    void handleTlsOptionsShouldSetClientAuthModeForValidValues() {
        tls.handleTlsOptions("client_authentication", "required", KEY_PREFIX, configFile);
        assertEquals("required", tls.getClientAuthMode());

        Tls otherTls = new Tls();
        otherTls.handleTlsOptions("client_authentication", "optional", KEY_PREFIX, configFile);
        assertEquals("optional", otherTls.getClientAuthMode());

        Tls thirdTls = new Tls();
        thirdTls.handleTlsOptions("client_authentication", "none", KEY_PREFIX, configFile);
        assertEquals("none", thirdTls.getClientAuthMode());
    }

    /**
     * Verifies that invalid client authentication modes are rejected.
     */
    @Test
    void handleTlsOptionsShouldNotSetClientAuthModeForInvalidValue() {
        tls.handleTlsOptions("client_authentication", "invalid", KEY_PREFIX, configFile);
        assertNull(tls.getClientAuthMode());
    }

    /**
     * Verifies that list based options are populated correctly.
     */
    @Test
    void handleTlsOptionsShouldPopulateListBasedOptions() {
        List<String> certificateAuthorityList = List.of("ca1.pem", "ca2.pem");
        List<String> protocolList = List.of("TLSv1.2", "TLSv1.3");
        List<String> cipherList = List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        List<String> allowList = List.of("10.0.0.1", "10.0.0.2");
        List<String> denyList = List.of("192.168.1.1");
        List<String> remoteAllowList = List.of("172.16.0.1");
        List<String> remoteDenyList = List.of("172.16.0.2");

        tls.handleTlsOptions("certificate_authorities", certificateAuthorityList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("supported_protocols", protocolList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("cipher_suites", cipherList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("filter.allow", allowList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("filter.deny", denyList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("remote_cluster.filter.allow", remoteAllowList, KEY_PREFIX, configFile);
        tls.handleTlsOptions("remote_cluster.filter.deny", remoteDenyList, KEY_PREFIX, configFile);

        assertEquals(certificateAuthorityList, tls.getCertificateAuthorities());
        assertEquals(protocolList, tls.getSupportedProtocols());
        assertEquals(cipherList, tls.getCiphers());
        assertEquals(allowList, tls.getAllowedIPs());
        assertEquals(denyList, tls.getDeniedIPs());
        assertEquals(remoteAllowList, tls.getRemoteClusterAllowedIPs());
        assertEquals(remoteDenyList, tls.getRemoteClusterDeniedIPs());
    }

    /**
     * Verifies that empty lists are ignored.
     */
    @Test
    void handleTlsOptionsShouldIgnoreEmptyLists() {
        tls.handleTlsOptions("certificate_authorities", List.of(), KEY_PREFIX, configFile);
        assertTrue(tls.getCertificateAuthorities().isEmpty());
    }

    /**
     * Verifies that lists with non string elements are rejected.
     */
    @Test
    void handleTlsOptionsShouldRejectListWithNonStringElements() {
        MigrationReport report = MigrationReport.shared;
        report.clear();
        List<Integer> invalidList = List.of(1, 2, 3);
        tls.handleTlsOptions("certificate_authorities", invalidList, KEY_PREFIX, configFile);
        assertTrue(tls.getCertificateAuthorities().isEmpty());
        assertTrue(report.getEntries("elasticsearch.yml", MigrationReport.Category.MANUAL)
                .stream()
                .anyMatch(entry -> (KEY_PREFIX + "certificate_authorities").equals(entry.parameter())));
    }
}
