/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.testsupport.TestBase;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link ElasticsearchYamlReader} populates the intermediate representation from an elasticsearch.yml.
 */
class ElasticsearchYamlReaderTest extends TestBase {

    /**
     * Verifies core TLS and global settings are parsed from a base config file.
     */
    @Test
    void shouldPopulateIntermediateRepresentationFromElasticsearchYaml() {
        IntermediateRepresentationElasticSearchYml ir = readIr("xpack_config/elasticsearch.yml");

        assertFalse(ir.getGlobal().getXpackSecEnabled(), "xpack.security.enabled should remain false");

        var http = ir.getSslTls().getHttp();
        assertTrue(http.getEnabled(), "HTTP TLS should be enabled");
        assertEquals("certs/http.p12", http.getKeystorePath(), "HTTP keystore path should be parsed");

        var transport = ir.getSslTls().getTransport();
        assertTrue(transport.getEnabled(), "Transport TLS should be enabled");
        assertEquals("certificate", transport.getVerificationMode(), "Transport verification mode should be set");
        assertEquals("certs/transport.p12", transport.getKeystorePath(), "Transport keystore path should be parsed");
        assertEquals("certs/transport.p12", transport.getTruststorePath(), "Transport truststore path should be parsed");
    }

    /**
     * Verifies that the rich configuration enables security features.
     */
    @Test
    void shouldParseExtendedElasticsearchConfiguration() {
        IntermediateRepresentationElasticSearchYml ir = readIr("xpack_config/elasticsearch-rich.yml");
        assertTrue(ir.getGlobal().getXpackSecEnabled(), "xpack.security.enabled should be true");
    }

    /**
     * Verifies full HTTP TLS settings are parsed from the rich configuration.
     */
    @Test
    void shouldParseRichHttpTls() {
        var http = readIr("xpack_config/elasticsearch-rich.yml").getSslTls().getHttp();
        assertTrue(http.getEnabled());
        assertEquals("PKCS12", http.getKeystoreType());
        assertEquals("certs/http-keystore.p12", http.getKeystorePath());
        assertEquals("httpStorePass", http.getKeystorePassword());
        assertEquals("httpKeyPass", http.getKeystoreKeyPassword());
        assertEquals("certs/http-truststore.p12", http.getTruststorePath());
        assertEquals("httpTrustPass", http.getTruststorePassword());
        assertEquals("PKCS12", http.getTruststoreType());
        assertEquals("certs/http.pem", http.getCertificatePath());
        assertEquals("certs/http.key", http.getPrivateKeyPath());
        assertEquals("httpSecret", http.getPrivateKeyPassword());
        assertEquals("full", http.getVerificationMode());
        assertEquals("optional", http.getClientAuthMode());
        assertEquals(List.of("certs/http-ca.pem", "certs/http-ca2.pem"), http.getCertificateAuthorities());
        assertEquals(List.of("TLSv1.2", "TLSv1.3"), http.getSupportedProtocols());
        assertEquals(List.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256"), http.getCiphers());
        assertEquals(List.of("127.0.0.1", "10.0.0.0/8", "localhost"), http.getAllowedIPs());
        assertEquals(List.of("0.0.0.0/0"), http.getDeniedIPs());
    }

    /**
     * Verifies full transport TLS settings are parsed from the rich configuration.
     */
    @Test
    void shouldParseRichTransportTls() {
        var transport = readIr("xpack_config/elasticsearch-rich.yml").getSslTls().getTransport();
        assertTrue(transport.getEnabled());
        assertEquals("certificate", transport.getVerificationMode());
        assertEquals("required", transport.getClientAuthMode());
        assertEquals("certs/transport-keystore.p12", transport.getKeystorePath());
        assertEquals("changeit", transport.getKeystorePassword());
        assertEquals("transKeyPass", transport.getKeystoreKeyPassword());
        assertEquals("PKCS12", transport.getKeystoreType());
        assertEquals("certs/transport-truststore.p12", transport.getTruststorePath());
        assertEquals("transTrustPass", transport.getTruststorePassword());
        assertEquals("PKCS12", transport.getTruststoreType());
        assertEquals(List.of("certs/ca1.pem", "certs/ca2.pem"), transport.getCertificateAuthorities());
        assertEquals(List.of("TLSv1.2", "TLSv1.3"), transport.getSupportedProtocols());
        assertEquals(List.of("TLS_AES_128_GCM_SHA256"), transport.getCiphers());
        assertEquals(List.of("10.0.0.1", "10.0.0.2"), transport.getAllowedIPs());
        assertEquals(List.of("_all"), transport.getDeniedIPs());
        assertEquals(List.of("172.20.0.0/16"), transport.getRemoteClusterAllowedIPs());
        assertEquals(List.of("172.21.0.0/16"), transport.getRemoteClusterDeniedIPs());
    }

    /**
     * Verifies transport profile filter settings are parsed from the rich configuration.
     */
    @Test
    void shouldParseTransportProfiles() {
        var sslTls = readIr("xpack_config/elasticsearch-rich.yml").getSslTls();
        assertEquals(List.of("192.168.1.10"), sslTls.getProfileAllowedIPs().get("edge"));
        assertEquals(List.of("192.168.1.0/24"), sslTls.getProfileDeniedIPs().get("edge"));
        assertEquals(List.of("10.0.0.0/8"), sslTls.getProfileAllowedIPs().get("backend"));
        assertEquals(List.of("172.16.0.0/12"), sslTls.getProfileDeniedIPs().get("backend"));
    }

    /**
     * Verifies authentication-related settings are parsed from the rich configuration.
     */
    @Test
    void shouldParseAuthSettings() {
        var auth = readIr("xpack_config/elasticsearch-rich.yml").getAuthent();
        assertTrue(auth.getTokenEnabled());
        assertEquals("30m", auth.getTokenTimeout());
        assertEquals("anon_user", auth.getAnonymousUserName());
        assertEquals("sg_anonymous", auth.getAnonymousRoles());
        assertTrue(auth.getAnonymousAuthzException());
        assertTrue(auth.getApiKeyEnabled());
        assertEquals("45m", auth.getApiKeyCacheTtl());
        assertEquals("8000", auth.getMaxTokens());
        assertEquals("pbkdf2", auth.getApiKeyInMemoryHashingAlgorithm());
        assertEquals("14d", auth.getApiKeyRetentionPeriod());
        assertEquals("2h", auth.getApiKeyDeleteInterval());
        assertEquals("1m", auth.getApiKeyDeleteTimeout());
        assertEquals("pbkdf2_1000", auth.getApiKeyHashingAlgorithm());
        assertEquals("pbkdf2_1000", auth.getPasswordHashingAlgoritm());
    }

    /**
     * Verifies file and native realms are parsed with expected attributes.
     */
    @Test
    void shouldParseFileAndNativeRealms() {
        var realms = readIr("xpack_config/elasticsearch-rich.yml").getAuthent().getRealms();
        assertEquals(7, realms.size());

        var nativeRealm = (RealmIR.NativeRealmIR) realms.get("native1");
        assertNotNull(nativeRealm);
        assertEquals("native", nativeRealm.getType());
        assertEquals("10m", nativeRealm.getCacheTtl());
        assertEquals(10000, nativeRealm.getCacheMaxUsers());
        assertTrue(nativeRealm.isEnabled());
        assertEquals(0, nativeRealm.getOrder());

        var fileRealm = (RealmIR.FileRealmIR) realms.get("file1");
        assertNotNull(fileRealm);
        assertEquals("users", fileRealm.getFilesUsers());
        assertEquals("users_roles", fileRealm.getFilesUsersRoles());
        assertTrue(fileRealm.isEnabled());
        assertEquals(1, fileRealm.getOrder());
    }

    /**
     * Verifies LDAP and PKI realms are parsed with expected attributes.
     */
    @Test
    void shouldParseLdapAndPkiRealms() {
        var realms = readIr("xpack_config/elasticsearch-rich.yml").getAuthent().getRealms();

        var ldap = (RealmIR.LdapRealmIR) realms.get("corp");
        assertNotNull(ldap);
        assertEquals("ldap", ldap.getType());
        assertEquals("ldaps://ldap.example.com:636", ldap.getUrl());
        assertEquals("cn=ldap-bind,ou=service,dc=example,dc=com", ldap.getBindDn());
        assertEquals("ou=users,dc=example,dc=com", ldap.getUserSearchBaseDn());
        assertEquals("(uid={0})", ldap.getUserSearchFilter());
        assertEquals("ou=groups,dc=example,dc=com", ldap.getGroupSearchBaseDn());
        assertEquals(2, ldap.getOrder());
        assertTrue(ldap.isEnabled());

        var pkiRealm = (RealmIR.PkiRealmIR) realms.get("pki1");
        assertNotNull(pkiRealm);
        assertEquals(List.of("config/ldap-ca.pem"), pkiRealm.getCertificateAuthorities());
        assertEquals("EMAILADDRESS=(.*)", pkiRealm.getUsernamePattern());
        assertEquals("mail", pkiRealm.getUsernameAttribute());
        assertEquals("certs/pki-trust.p12", pkiRealm.getTruststorePath());
        assertEquals("PKCS12", pkiRealm.getTruststoreType());
        assertEquals("changeit", pkiRealm.getTruststorePassword());
        assertEquals(Boolean.TRUE, pkiRealm.getDelegationEnabled());
        assertTrue(pkiRealm.isEnabled());
        assertEquals(3, pkiRealm.getOrder());
    }

    /**
     * Verifies OIDC, SAML, and Kerberos realms are parsed with expected attributes.
     */
    @Test
    void shouldParseOidcSamlAndKerberosRealms() {
        var realms = readIr("xpack_config/elasticsearch-rich.yml").getAuthent().getRealms();

        var oidcRealm = (RealmIR.OidcRealmIR) realms.get("oidc1");
        assertNotNull(oidcRealm);
        assertEquals("sg-client", oidcRealm.getRpClientId());
        assertEquals("code", oidcRealm.getRpResponseType());
        assertEquals("https://example.com/logout", oidcRealm.getRpPostLogoutRedirectUri());
        assertEquals("https://issuer.example.com", oidcRealm.getOpIssuer());
        assertEquals("https://issuer.example.com/auth", oidcRealm.getOpAuthEndpoint());
        assertEquals("https://issuer.example.com/token", oidcRealm.getOpTokenEndpoint());
        assertEquals("https://issuer.example.com/keys.jwks", oidcRealm.getOpJwkSetPath());
        assertEquals("sub", oidcRealm.getClaimPrincipal());
        assertEquals("name", oidcRealm.getClaimName());
        assertEquals("email", oidcRealm.getClaimMail());
        assertEquals("groups", oidcRealm.getClaimGroups());
        assertTrue(oidcRealm.isEnabled());
        assertEquals(5, oidcRealm.getOrder());

        var samlRealm = (RealmIR.SamlRealmIR) realms.get("saml1");
        assertNotNull(samlRealm);
        assertEquals("saml", samlRealm.getType());
        assertEquals("config/idp.xml", samlRealm.getIdpMetadataPath());
        assertEquals("https://es.example.com/", samlRealm.getSpEntityID());
        assertEquals("https://es.example.com/api/security/saml/callback", samlRealm.getSpAcs());
        assertEquals("nameid", samlRealm.getAttributesPrincipal());
        assertTrue(samlRealm.isEnabled());
        assertEquals(4, samlRealm.getOrder());

        var kerberosRealm = (RealmIR.KerberosRealmIR) realms.get("krb1");
        assertNotNull(kerberosRealm);
        assertEquals("kerberos", kerberosRealm.getType());
        assertEquals("/etc/krb5.keytab", kerberosRealm.getKeytabPath());
        assertEquals("HTTP/es.example.com@EXAMPLE.COM", kerberosRealm.getPrincipal());
        assertEquals(Boolean.TRUE, kerberosRealm.getKrbDebug());
        assertEquals(Boolean.FALSE, kerberosRealm.getRemoveRealmName());
        assertTrue(kerberosRealm.isEnabled());
        assertEquals(6, kerberosRealm.getOrder());
    }

    /**
     * Verifies that unsupported or unknown settings are surfaced as migration report entries rather than silently ignored.
     */
    @Test
    void shouldReportUnsupportedSettings() {
        MigrationReport report = newEmptyReport();
        IntermediateRepresentationElasticSearchYml ir = readIrWithReport("xpack_config/elasticsearch-unsupported.yml", report);

        // known fields are still parsed
        assertTrue(ir.getGlobal().getXpackSecEnabled());
        assertTrue(ir.getSslTls().getHttp().getEnabled());
        assertTrue(ir.getAuthent().getApiKeyEnabled());

        // unsupported realms should emit manual/unknown entries
        assertTrue(hasAnyEntry(report, "elasticsearch.yml", MigrationReport.Category.MANUAL));

        // extra OIDC keys not mapped today should be reported
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.authc.realms.oidc.oidc1.op.userinfo_endpoint"));

        // transport/http filter flags outside our TLS handlers should be reported
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.transport.filter.enabled"));
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.http.filter.enabled"));
    }

    /**
     * Verifies malformed YAML inputs are reported as warnings.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportMalformedYaml(@TempDir Path tempDir) throws IOException {
        MigrationReport report = newEmptyReport();
        File config = tempDir.resolve("elasticsearch.yml").toFile();
        Files.writeString(config.toPath(), "xpack.security: [");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new ElasticsearchYamlReader(config, new IntermediateRepresentationElasticSearchYml());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "origin"));
    }

    /**
     * Verifies type mismatches are surfaced as unknown keys in the report.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportWrongTypeForTlsFlag(@TempDir Path tempDir) throws IOException {
        MigrationReport report = newEmptyReport();
        File config = tempDir.resolve("elasticsearch.yml").toFile();
        Files.writeString(config.toPath(), "xpack.security.http.ssl.enabled: \"true\"");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new ElasticsearchYamlReader(config, new IntermediateRepresentationElasticSearchYml());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.http.ssl.enabled"));
    }

    /**
     * Reads an elasticsearch.yml resource into a fresh intermediate representation.
     *
     * @param resourceName resource path
     * @return populated intermediate representation
     */
    private IntermediateRepresentationElasticSearchYml readIr(String resourceName) {
        Path configPath = resolveResourcePath(resourceName);
        MigrationReport previousReport = MigrationReport.shared;
        try {
            MigrationReport.shared = newEmptyReport();
            IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();
            new ElasticsearchYamlReader(new File(configPath.toString()), ir);
            return ir;
        } finally {
            MigrationReport.shared = previousReport;
        }
    }

    /**
     * Reads an elasticsearch.yml resource into an intermediate representation using a given report.
     *
     * @param resourceName resource path
     * @param report report to collect warnings
     * @return populated intermediate representation
     */
    private IntermediateRepresentationElasticSearchYml readIrWithReport(String resourceName, MigrationReport report) {
        Path configPath = resolveResourcePath(resourceName);
        MigrationReport previousReport = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();
            new ElasticsearchYamlReader(new File(configPath.toString()), ir);
            return ir;
        } finally {
            MigrationReport.shared = previousReport;
        }
    }

    /**
     * Creates a fresh report instance using reflection.
     *
     * @return empty migration report
     */
    private MigrationReport newEmptyReport() {
        try {
            Constructor<MigrationReport> ctor = MigrationReport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create isolated MigrationReport", e);
        }
    }

    /**
     * Checks whether the report has an entry with the given parameter.
     *
     * @param report report instance
     * @param file file name
     * @param category report category
     * @param parameter parameter to match
     * @return true when an entry exists
     */
    private boolean hasReportEntry(MigrationReport report, String file, MigrationReport.Category category, String parameter) {
        return report.getEntries(file, category).stream().anyMatch(e -> parameter.equals(e.getParameter()));
    }

    /**
     * Checks whether the report has any entries for a file and category.
     *
     * @param report report instance
     * @param file file name
     * @param category report category
     * @return true when at least one entry exists
     */
    private boolean hasAnyEntry(MigrationReport report, String file, MigrationReport.Category category) {
        return !report.getEntries(file, category).isEmpty();
    }
}
