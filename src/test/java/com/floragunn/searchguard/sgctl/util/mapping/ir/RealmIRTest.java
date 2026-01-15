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

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RealmIR} and its concrete subclasses.
 */
class RealmIRTest {

    private static final File DUMMY_CONFIG = new File("elasticsearch.yml");
    private static final String PREFIX = "xpack.security.authc.realms.test.";

    /**
     * Verifies that {@link RealmIR#create(String, String)} returns the correct subclass type
     * for all supported realm types and a fallback implementation for unknown types.
     */
    @Test
    void createShouldReturnCorrectSubclassForKnownAndUnknownTypes() {
        assertInstanceOf(RealmIR.LdapRealmIR.class, RealmIR.create("ldap", "r1"));
        assertInstanceOf(RealmIR.FileRealmIR.class, RealmIR.create("file", "r2"));
        assertInstanceOf(RealmIR.NativeRealmIR.class, RealmIR.create("native", "r3"));
        assertInstanceOf(RealmIR.SamlRealmIR.class, RealmIR.create("saml", "r4"));
        assertInstanceOf(RealmIR.PkiRealmIR.class, RealmIR.create("pki", "r5"));
        assertInstanceOf(RealmIR.OidcRealmIR.class, RealmIR.create("oidc", "r6"));
        assertInstanceOf(RealmIR.KerberosRealmIR.class, RealmIR.create("kerberos", "r7"));

        RealmIR unknown = RealmIR.create("someUnknownType", "r8");
        assertNotNull(unknown);
        assertEquals("someUnknownType", unknown.getType());
        assertEquals("r8", unknown.getName());
    }

    /**
     * Verifies that LDAP realm attributes are mapped to the corresponding fields.
     */
    @Test
    void ldapRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("ldap", "ldap1");
        assertInstanceOf(RealmIR.LdapRealmIR.class, realm);
        RealmIR.LdapRealmIR ldapRealm = getLdapRealmIR((RealmIR.LdapRealmIR) realm);

        assertEquals("ldap", ldapRealm.getType());
        assertEquals("ldap1", ldapRealm.getName());
        assertTrue(ldapRealm.isEnabled());
        assertEquals(10, ldapRealm.getOrder());
        assertEquals("ldap://example.com", ldapRealm.getUrl());
        assertEquals("cn=admin,dc=example,dc=com", ldapRealm.getBindDn());
        assertEquals("ou=people,dc=example,dc=com", ldapRealm.getUserSearchBaseDn());
        assertEquals("(uid={0})", ldapRealm.getUserSearchFilter());
        assertEquals("ou=groups,dc=example,dc=com", ldapRealm.getGroupSearchBaseDn());
    }

    /**
     * Populates a test LDAP realm with sample attributes.
     *
     * @param realm LDAP realm instance
     * @return populated LDAP realm
     */
    private static RealmIR.LdapRealmIR getLdapRealmIR(RealmIR.LdapRealmIR realm) {

        realm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("url", "ldap://example.com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("bindDn", "cn=admin,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("user_search.base_dn", "ou=people,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("user_search.filter", "(uid={0})", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("group_search.base_dn", "ou=groups,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("order", 10, PREFIX, DUMMY_CONFIG);
        return realm;
    }

    /**
     * Verifies that file realm attributes are mapped to the corresponding fields.
     */
    @Test
    void fileRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("file", "file1");
        assertInstanceOf(RealmIR.FileRealmIR.class, realm);
        RealmIR.FileRealmIR fileRealm = (RealmIR.FileRealmIR) realm;

        fileRealm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        fileRealm.handleAttribute("files.users", "users.yml", PREFIX, DUMMY_CONFIG);
        fileRealm.handleAttribute("files.users_roles", "users_roles.yml", PREFIX, DUMMY_CONFIG);
        fileRealm.handleAttribute("order", 20, PREFIX, DUMMY_CONFIG);

        assertEquals("file", fileRealm.getType());
        assertEquals("file1", fileRealm.getName());
        assertTrue(fileRealm.isEnabled());
        assertEquals(20, fileRealm.getOrder());
        assertEquals("users.yml", fileRealm.getFilesUsers());
        assertEquals("users_roles.yml", fileRealm.getFilesUsersRoles());
    }

    /**
     * Verifies that native realm attributes are mapped to the corresponding fields.
     */
    @Test
    void nativeRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("native", "native1");
        assertInstanceOf(RealmIR.NativeRealmIR.class, realm);
        RealmIR.NativeRealmIR nativeRealm = (RealmIR.NativeRealmIR) realm;

        nativeRealm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        nativeRealm.handleAttribute("cache.ttl", "5m", PREFIX, DUMMY_CONFIG);
        nativeRealm.handleAttribute("order", 30, PREFIX, DUMMY_CONFIG);
        nativeRealm.handleAttribute("cache.max_users", 100, PREFIX, DUMMY_CONFIG);

        assertEquals("native", nativeRealm.getType());
        assertEquals("native1", nativeRealm.getName());
        assertTrue(nativeRealm.isEnabled());
        assertEquals(30, nativeRealm.getOrder());
        assertEquals("5m", nativeRealm.getCacheTtl());
        assertEquals(100, nativeRealm.getCacheMaxUsers());
    }

    /**
     * Verifies that SAML realm attributes are mapped to the corresponding fields.
     */
    @Test
    void samlRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("saml", "saml1");
        assertInstanceOf(RealmIR.SamlRealmIR.class, realm);
        RealmIR.SamlRealmIR samlRealm = getSamlRealmIR((RealmIR.SamlRealmIR) realm);

        assertEquals("saml", samlRealm.getType());
        assertEquals("saml1", samlRealm.getName());
        assertTrue(samlRealm.isEnabled());
        assertEquals(40, samlRealm.getOrder());
        assertEquals("/path/to/metadata.xml", samlRealm.getIdpMetadataPath());
        assertEquals("https://sp.example.com", samlRealm.getSpEntityID());
        assertEquals("https://sp.example.com/acs", samlRealm.getSpAcs());
        assertEquals("uid", samlRealm.getAttributesPrincipal());
    }

    /**
     * Populates a test SAML realm with sample attributes.
     *
     * @param realm SAML realm instance
     * @return populated SAML realm
     */
    private static RealmIR.SamlRealmIR getSamlRealmIR(RealmIR.SamlRealmIR realm) {

        realm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("idp.metadata.path", "/path/to/metadata.xml", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("sp.entity_id", "https://sp.example.com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("sp.acs", "https://sp.example.com/acs", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("attributes.principal", "uid", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("order", 40, PREFIX, DUMMY_CONFIG);
        return realm;
    }

    /**
     * Verifies that PKI realm attributes are mapped to the corresponding fields.
     */
    @Test
    void pkiRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("pki", "pki1");
        assertInstanceOf(RealmIR.PkiRealmIR.class, realm);
        RealmIR.PkiRealmIR pkiRealm = (RealmIR.PkiRealmIR) realm;

        pkiRealm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("delegation.enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("username_pattern", "CN=(.*?)(?:,|$)", PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("username_attribute", "cn", PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("truststore.path", "/path/to/truststore.jks", PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("truststore.type", "jks", PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("truststore.password", "secret", PREFIX, DUMMY_CONFIG);
        pkiRealm.handleAttribute("order", 50, PREFIX, DUMMY_CONFIG);
        List<String> cas = Arrays.asList("ca1.pem", "ca2.pem");
        pkiRealm.handleAttribute("certificate_authorities", cas, PREFIX, DUMMY_CONFIG);

        assertEquals("pki", pkiRealm.getType());
        assertEquals("pki1", pkiRealm.getName());
        assertTrue(pkiRealm.isEnabled());
        assertEquals(50, pkiRealm.getOrder());
        assertEquals(Boolean.TRUE, pkiRealm.getDelegationEnabled());
        assertEquals("CN=(.*?)(?:,|$)", pkiRealm.getUsernamePattern());
        assertEquals("cn", pkiRealm.getUsernameAttribute());
        assertEquals("/path/to/truststore.jks", pkiRealm.getTruststorePath());
        assertEquals("jks", pkiRealm.getTruststoreType());
        assertEquals("secret", pkiRealm.getTruststorePassword());
        assertEquals(cas, pkiRealm.getCertificateAuthorities());
    }

    /**
     * Verifies empty certificate authority lists are ignored.
     */
    @Test
    void pkiRealmShouldIgnoreEmptyCertificateAuthorities() {
        RealmIR realm = RealmIR.create("pki", "pki-empty");
        assertInstanceOf(RealmIR.PkiRealmIR.class, realm);
        RealmIR.PkiRealmIR pkiRealm = (RealmIR.PkiRealmIR) realm;

        pkiRealm.handleAttribute("certificate_authorities", List.of(), PREFIX, DUMMY_CONFIG);

        assertTrue(pkiRealm.getCertificateAuthorities().isEmpty());
    }

    /**
     * Verifies that OIDC realm attributes are mapped to the corresponding fields.
     */
    @Test
    void oidcRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("oidc", "oidc1");
        assertInstanceOf(RealmIR.OidcRealmIR.class, realm);
        RealmIR.OidcRealmIR oidcRealm = getOidcRealmIR((RealmIR.OidcRealmIR) realm);

        assertEquals("oidc", oidcRealm.getType());
        assertEquals("oidc1", oidcRealm.getName());
        assertTrue(oidcRealm.isEnabled());
        assertEquals(60, oidcRealm.getOrder());
        assertEquals("client-id", oidcRealm.getRpClientId());
        assertEquals("code", oidcRealm.getRpResponseType());
        assertEquals("https://client/logout", oidcRealm.getRpPostLogoutRedirectUri());
        assertEquals("https://issuer.example.com", oidcRealm.getOpIssuer());
        assertEquals("https://issuer.example.com/authorize", oidcRealm.getOpAuthEndpoint());
        assertEquals("https://issuer.example.com/token", oidcRealm.getOpTokenEndpoint());
        assertEquals("/path/to/jwks.json", oidcRealm.getOpJwkSetPath());
        assertEquals("sub", oidcRealm.getClaimPrincipal());
        assertEquals("name", oidcRealm.getClaimName());
        assertEquals("email", oidcRealm.getClaimMail());
        assertEquals("groups", oidcRealm.getClaimGroups());
    }

    /**
     * Populates a test OIDC realm with sample attributes.
     *
     * @param realm OIDC realm instance
     * @return populated OIDC realm
     */
    private static RealmIR.OidcRealmIR getOidcRealmIR(RealmIR.OidcRealmIR realm) {

        realm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("rp.client_id", "client-id", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("rp.response_type", "code", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("rp.post_logout_redirect_uri", "https://client/logout", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("op.issuer", "https://issuer.example.com", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("op.authorization_endpoint", "https://issuer.example.com/authorize", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("op.token_endpoint", "https://issuer.example.com/token", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("op.jwkset_path", "/path/to/jwks.json", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("claims.principal", "sub", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("claims.name", "name", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("claims.mail", "email", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("claims.groups", "groups", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("order", 60, PREFIX, DUMMY_CONFIG);
        return realm;
    }

    /**
     * Verifies that Kerberos realm attributes are mapped to the corresponding fields.
     */
    @Test
    void kerberosRealmShouldHandleKnownAttributes() {
        RealmIR realm = RealmIR.create("kerberos", "krb1");
        assertInstanceOf(RealmIR.KerberosRealmIR.class, realm);
        RealmIR.KerberosRealmIR krbRealm = getKerberosRealmIR((RealmIR.KerberosRealmIR) realm);

        assertEquals("kerberos", krbRealm.getType());
        assertEquals("krb1", krbRealm.getName());
        assertTrue(krbRealm.isEnabled());
        assertEquals(70, krbRealm.getOrder());
        assertEquals(Boolean.TRUE, krbRealm.getKrbDebug());
        assertEquals(Boolean.FALSE, krbRealm.getRemoveRealmName());
        assertEquals("/path/to/keytab", krbRealm.getKeytabPath());
        assertEquals("HTTP/host@example.com@EXAMPLE.COM", krbRealm.getPrincipal());
    }

    /**
     * Populates a test Kerberos realm with sample attributes.
     *
     * @param realm Kerberos realm instance
     * @return populated Kerberos realm
     */
    private static RealmIR.KerberosRealmIR getKerberosRealmIR(RealmIR.KerberosRealmIR realm) {

        realm.handleAttribute("enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("krb.debug", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("remove_realm_name", Boolean.FALSE, PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("keytab.path", "/path/to/keytab", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("principal", "HTTP/host@example.com@EXAMPLE.COM", PREFIX, DUMMY_CONFIG);
        realm.handleAttribute("order", 70, PREFIX, DUMMY_CONFIG);
        return realm;
    }

    /**
     * Verifies that unknown realm implementation accepts attributes without throwing
     * and keeps basic metadata.
     */
    @Test
    void unknownRealmShouldAcceptAttributesWithoutThrowing() {
        RealmIR realm = RealmIR.create("unknown", "test");

        assertEquals("unknown", realm.getType());
        assertEquals("test", realm.getName());

        assertDoesNotThrow(() ->
                realm.handleAttribute("some.attribute", "value", PREFIX, DUMMY_CONFIG)
        );
    }
}
