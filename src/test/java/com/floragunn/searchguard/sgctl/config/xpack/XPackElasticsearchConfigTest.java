package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parsing X-Pack elasticsearch.yml configuration.
 */
public class XPackElasticsearchConfigTest {

    @Test
    public void testParseBasicRealms() throws IOException, ConfigValidationException {
        var node = read("/xpack_migrate/elasticsearch/basic_realms.yml");
        var config = XPackElasticsearchConfig.parse(node, Parser.Context.get());

        assertNotNull(config);
        assertTrue(config.security().enabled());
        assertNotNull(config.security().authc());

        var realms = config.security().authc().realms();
        assertEquals(4, realms.size(), "Should have 4 realms: native, file, ldap, active_directory");

        // Verify native realm
        var native1 = realms.get("native.native1");
        assertNotNull(native1);
        assertInstanceOf(Realm.NativeRealm.class, native1);
        assertEquals("native", native1.type());
        assertEquals("native1", native1.name());
        assertEquals(0, native1.order());
        assertTrue(native1.enabled());

        // Verify file realm
        var file1 = realms.get("file.file1");
        assertNotNull(file1);
        assertInstanceOf(Realm.FileRealm.class, file1);
        assertEquals("file", file1.type());
        assertEquals("file1", file1.name());
        assertEquals(1, file1.order());
        assertTrue(file1.enabled());

        // Verify LDAP 
        var ldap1 = realms.get("ldap.ldap1");
        assertNotNull(ldap1);
        assertInstanceOf(Realm.LdapRealm.class, ldap1);
        assertEquals("ldap", ldap1.type());
        assertEquals("ldap1", ldap1.name());
        assertEquals(2, ldap1.order());
        assertTrue(ldap1.enabled());
        
        var ldapRealm = (Realm.LdapRealm) ldap1;
        assertEquals(1, ldapRealm.url().size());
        assertEquals("ldaps://ldap.example.com:636", ldapRealm.url().get(0));
        assertEquals("cn=admin,dc=example,dc=com", ldapRealm.bindDn());
        assertEquals("ldapsecret", ldapRealm.bindPassword());
        assertEquals("ou=users,dc=example,dc=com", ldapRealm.userSearchBaseDn());
        assertEquals("(uid={0})", ldapRealm.userSearchFilter());
        assertEquals("ou=groups,dc=example,dc=com", ldapRealm.groupSearchBaseDn());
        assertFalse(ldapRealm.unmappedGroupsAsRoles());

        // Verify Active Directory 
        var ad1 = realms.get("active_directory.ad1");
        assertNotNull(ad1);
        assertInstanceOf(Realm.ActiveDirectoryRealm.class, ad1);
        assertEquals("active_directory", ad1.type());
        assertEquals("ad1", ad1.name());
        assertEquals(3, ad1.order());
        assertTrue(ad1.enabled());
        
        var adRealm = (Realm.ActiveDirectoryRealm) ad1;
        assertEquals("example.com", adRealm.domainName());
        assertEquals(1, adRealm.url().size());
        assertEquals("ldaps://ad.example.com:636", adRealm.url().get(0));
        assertEquals("cn=svc_account,dc=example,dc=com", adRealm.bindDn());
        assertEquals("dc=example,dc=com", adRealm.userSearchBaseDn());
        assertTrue(adRealm.unmappedGroupsAsRoles());
    }

    @Test
    public void testParseEmptyRealms() throws IOException, ConfigValidationException {
        String yaml = """
            xpack:
              security:
                enabled: true
                authc:
                  realms: {}
            """;
        var node = DocNode.wrap(DocReader.yaml().read(yaml));
        var config = XPackElasticsearchConfig.parse(node, Parser.Context.get());

        assertNotNull(config);
        assertTrue(config.security().enabled());
        assertEquals(0, config.security().authc().realms().size());
    }

    @Test
    public void testParseDisabledRealm() throws IOException, ConfigValidationException {
        String yaml = """
            xpack:
              security:
                enabled: false
                authc:
                  realms:
                    native:
                      native1:
                        order: 0
                        enabled: false
            """;
        var node = DocNode.wrap(DocReader.yaml().read(yaml));
        var config = XPackElasticsearchConfig.parse(node, Parser.Context.get());

        assertNotNull(config);
        assertFalse(config.security().enabled());
        
        var native1 = config.security().authc().realms().get("native.native1");
        assertNotNull(native1);
        assertFalse(native1.enabled());
    }

    @Test
    public void testLdapWithoutGroupSearch() throws IOException, ConfigValidationException {
        String yaml = """
            xpack:
              security:
                enabled: true
                authc:
                  realms:
                    ldap:
                      ldapNoGroups:
                        order: 0
                        enabled: true
                        url: ["ldaps://ldap.example.com:636"]
                        bind_dn: "cn=admin,dc=example,dc=com"
                        bind_password: "secret"
                        user_search:
                          base_dn: "ou=users,dc=example,dc=com"
            """;

        var node = DocNode.wrap(DocReader.yaml().read(yaml));
        var config = XPackElasticsearchConfig.parse(node, Parser.Context.get());

        var ldap = config.security().authc().realms().get("ldap.ldapNoGroups");
        assertNotNull(ldap);
        assertInstanceOf(Realm.LdapRealm.class, ldap);
        var ldapRealm = (Realm.LdapRealm) ldap;
        assertEquals("secret", ldapRealm.bindPassword());
        assertNull(ldapRealm.groupSearchBaseDn());
        assertNull(ldapRealm.groupSearchScope());
        assertNull(ldapRealm.groupSearchFilter());
    }

    private DocNode read(String path) throws IOException, DocumentParseException {
        try (var in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Resource not found: " + path);
            return DocNode.wrap(DocReader.yaml().read(in));
        }
    }
}
