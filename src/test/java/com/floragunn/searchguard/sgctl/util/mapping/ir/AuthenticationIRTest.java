package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.AuthenticationIR;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AuthenticationIR}.
 */
class AuthenticationIRTest {

    private static final String PREFIX = "xpack.security.authc.";
    private static final File DUMMY_CONFIG = new File("elasticsearch.yml");

    /**
     * Verifies that API key related options are correctly mapped to the AuthenticationIR.
     */
    @Test
    void handleOptionsShouldSetApiKeyFlagsAndCacheSettings() {
        AuthenticationIR ir = new AuthenticationIR();

        ir.handleOptions("api_key.enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        ir.handleOptions("api_key.cache.ttl", "5m", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("api_key.cache.max_keys", "1000", PREFIX, DUMMY_CONFIG);

        assertTrue(ir.getApiKeyEnabled(), "API key must be enabled");
        assertEquals("5m", ir.getApiKeyCacheTtl(), "Unexpected cache TTL");
        // getMaxTokens() liefert intern maxKeys
        assertEquals("1000", ir.getMaxTokens(), "Unexpected max_keys value");
    }

    /**
     * Verifies that realm options are delegated to the corresponding RealmIR instance.
     */
    @Test
    void handleOptionsShouldCreateAndPopulateLdapRealm() {
        Map<String, RealmIR> realms = getStringRealmIRMap();
        assertEquals(1, realms.size(), "Exactly one realm should be present");

        RealmIR realm = realms.get("ldap1");
        assertNotNull(realm, "Realm 'ldap1' must exist");
        assertEquals("ldap", realm.getType(), "Realm type must be 'ldap'");
        assertEquals("ldap1", realm.getName(), "Realm name must be 'ldap1'");
        assertEquals(5, realm.getOrder(), "Unexpected realm order");
        assertTrue(realm.isEnabled(), "Realm should be enabled");

        assertInstanceOf(RealmIR.LdapRealmIR.class, realm, "Realm must be an LDAP realm");
        RealmIR.LdapRealmIR ldapRealm = (RealmIR.LdapRealmIR) realm;
        assertEquals("ldap://example.com", ldapRealm.getUrl());
        assertEquals("cn=admin,dc=example,dc=com", ldapRealm.getBindDn());
        assertEquals("ou=people,dc=example,dc=com", ldapRealm.getUserSearchBaseDn());
        assertEquals("(uid={0})", ldapRealm.getUserSearchFilter());
        assertEquals("ou=groups,dc=example,dc=com", ldapRealm.getGroupSearchBaseDn());
    }

    /**
     * Verifies the realms map is immutable to callers.
     */
    @Test
    void realmsMapShouldBeImmutable() {
        AuthenticationIR ir = new AuthenticationIR();
        Map<String, RealmIR> realms = ir.getRealms();

        assertNotNull(realms);
        assertThrows(UnsupportedOperationException.class, () -> realms.put("x", RealmIR.create("file", "x")));
    }

    /**
     * Builds an LDAP realm map from test configuration entries.
     *
     * @return realm map populated with LDAP attributes
     */
    private static Map<String, RealmIR> getStringRealmIRMap() {
        AuthenticationIR ir = new AuthenticationIR();

        ir.handleOptions("realms.ldap.ldap1.enabled", Boolean.TRUE, PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.url", "ldap://example.com", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.bindDn", "cn=admin,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.user_search.base_dn", "ou=people,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.user_search.filter", "(uid={0})", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.group_search.base_dn", "ou=groups,dc=example,dc=com", PREFIX, DUMMY_CONFIG);
        ir.handleOptions("realms.ldap.ldap1.order", 5, PREFIX, DUMMY_CONFIG);

        return ir.getRealms();
    }
}
