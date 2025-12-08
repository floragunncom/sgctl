package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.testsupport.TestBase;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that {@link ElasticsearchYamlReader} populates the intermediate representation from an elasticsearch.yml.
 */
class ElasticsearchYamlReaderTest extends TestBase {

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

    @Test
    void shouldParseExtendedElasticsearchConfiguration() {
        IntermediateRepresentationElasticSearchYml ir = readIr("xpack_config/elasticsearch-rich.yml");

        assertTrue(ir.getGlobal().getXpackSecEnabled(), "xpack.security.enabled should be true");

        var http = ir.getSslTls().getHttp();
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

        var transport = ir.getSslTls().getTransport();
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

        assertEquals(List.of("192.168.1.10"), ir.getSslTls().getProfileAllowedIPs().get("edge"));
        assertEquals(List.of("192.168.1.0/24"), ir.getSslTls().getProfileDeniedIPs().get("edge"));
        assertEquals(List.of("10.0.0.0/8"), ir.getSslTls().getProfileAllowedIPs().get("backend"));
        assertEquals(List.of("172.16.0.0/12"), ir.getSslTls().getProfileDeniedIPs().get("backend"));

        var auth = ir.getAuthent();
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

        var realms = auth.getRealms();
        assertEquals(7, realms.size());

        var nativeRealm = (RealmIR.NativeRealmIR) realms.get("native1");
        assertNotNull(nativeRealm);
        assertEquals("native", nativeRealm.getType());
        assertEquals("10m", nativeRealm.getCacheTtl());
        assertEquals(10000, nativeRealm.getCacheMaxUsers());
        assertTrue(nativeRealm.isEnabled());
        assertEquals(0, nativeRealm.getOrder());

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

        var fileRealm = (RealmIR.FileRealmIR) realms.get("file1");
        assertNotNull(fileRealm);
        assertEquals("users", fileRealm.getFilesUsers());
        assertEquals("users_roles", fileRealm.getFilesUsersRoles());
        assertTrue(fileRealm.isEnabled());
        assertEquals(1, fileRealm.getOrder());

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

        // extra SAML/OIDC keys not mapped today should be reported
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.authc.realms.saml.saml1.idp.entity_id"));
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.authc.realms.oidc.oidc1.op.userinfo_endpoint"));

        // transport/http filter flags outside our TLS handlers should be reported
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.transport.filter.enabled"));
        assertTrue(hasReportEntry(report, "elasticsearch.yml", MigrationReport.Category.WARNING, "xpack.security.http.filter.enabled"));
    }

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

    private MigrationReport newEmptyReport() {
        try {
            Constructor<MigrationReport> ctor = MigrationReport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create isolated MigrationReport", e);
        }
    }

    private boolean hasReportEntry(MigrationReport report, String file, MigrationReport.Category category, String parameter) {
        return report.getEntries(file, category).stream().anyMatch(e -> parameter.equals(e.getParameter()));
    }

    private boolean hasAnyEntry(MigrationReport report, String file, MigrationReport.Category category) {
        return !report.getEntries(file, category).isEmpty();
    }
}
