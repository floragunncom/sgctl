package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for parsing X-Pack elasticsearch.yml configuration. */
public class XPackElasticsearchConfigTest {

  @Test
  public void testParseBasicRealms() throws IOException, ConfigValidationException {
    var config = readConfig("/xpack_migrate/elasticsearch/basic_realms.yml");

    assertNotNull(config);
    var security = config.security().get();
    assertTrue(security.enabled().get());

    var authC = security.authc().get().orElseThrow();
    var realms = authC.realms().get();
    assertEquals(4, realms.size(), "Should have 4 realms: native, file, ldap, active_directory");

    // Verify native realm
    var native1 = realms.get("native").get().get("native1").get();
    assertNotNull(native1);
    assertInstanceOf(Realm.NativeRealm.class, native1);
    assertEquals("native", native1.type().get());
    assertEquals("native1", native1.name().get());
    assertEquals(0, native1.order().get());
    assertTrue(native1.enabled().get());

    // Verify file realm
    var file1 = realms.get("file").get().get("file1").get();
    assertNotNull(file1);
    assertInstanceOf(Realm.FileRealm.class, file1);
    assertEquals("file", file1.type().get());
    assertEquals("file1", file1.name().get());
    assertEquals(1, file1.order().get());
    assertTrue(file1.enabled().get());

    // Verify LDAP
    var ldap1 = realms.get("ldap").get().get("ldap1").get();
    assertNotNull(ldap1);
    assertInstanceOf(Realm.LdapRealm.class, ldap1);
    assertEquals("ldap", ldap1.type().get());
    assertEquals("ldap1", ldap1.name().get());
    assertEquals(2, ldap1.order().get());
    assertTrue(ldap1.enabled().get());

    var ldapRealm = (Realm.LdapRealm) ldap1;
    assertEquals(1, ldapRealm.url().get().size());
    assertEquals("ldaps://ldap.example.com:636", ldapRealm.url().get().get(0).get());
    assertEquals("cn=admin,dc=example,dc=com", ldapRealm.bindDn().get().orElseThrow());
    assertEquals("ldapsecret", ldapRealm.bindPassword().get());
    assertEquals("ou=users,dc=example,dc=com", ldapRealm.userSearchBaseDn().get().orElseThrow());
    assertEquals("(uid={0})", ldapRealm.userSearchFilter().get());
    assertEquals("ou=groups,dc=example,dc=com", ldapRealm.groupSearchBaseDn().get().orElseThrow());
    assertFalse(ldapRealm.unmappedGroupsAsRoles().get());

    // Verify Active Directory
    var ad1 = realms.get("active_directory").get().get("ad1").get();
    assertNotNull(ad1);
    assertInstanceOf(Realm.ActiveDirectoryRealm.class, ad1);
    assertEquals("active_directory", ad1.type().get());
    assertEquals("ad1", ad1.name().get());
    assertEquals(3, ad1.order().get());
    assertTrue(ad1.enabled().get());

    var adRealm = (Realm.ActiveDirectoryRealm) ad1;
    assertEquals("example.com", adRealm.domainName().get());
    var url = adRealm.url().get().orElseThrow();
    assertEquals(1, url.size());
    assertEquals("ldaps://ad.example.com:636", url.get(0).get());
    assertEquals("cn=svc_account,dc=example,dc=com", adRealm.bindDn().get().orElseThrow());
    assertEquals("dc=example,dc=com", adRealm.userSearchBaseDn().get().orElseThrow());
    assertTrue(adRealm.unmappedGroupsAsRoles().get());
  }

  @Test
  public void testParseEmptyRealms() throws IOException, ConfigValidationException {
    String yaml =
        """
            xpack:
              security:
                enabled: true
                authc:
                  realms: {}
            """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var config = parseConfig(node);

    assertNotNull(config);
    assertTrue(config.security().get().enabled().get());
    assertEquals(0, config.security().get().authc().get().orElseThrow().realms().get().size());
  }

  @Test
  public void testParseDisabledRealm() throws IOException, ConfigValidationException {
    String yaml =
        """
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
    var config = parseConfig(node);

    assertNotNull(config);
    assertFalse(config.security().get().enabled().get());

    var native1 =
        config
            .security()
            .get()
            .authc()
            .get()
            .orElseThrow()
            .realms()
            .get()
            .get("native")
            .get()
            .get("native1")
            .get();
    assertNotNull(native1);
    assertFalse(native1.enabled().get());
  }

  @Test
  public void testLdapAuthorizationRealmsString() throws IOException, ConfigValidationException {
    String yaml =
        """
            xpack:
              security:
                enabled: true
                authc:
                  realms:
                    ldap:
                      ldap1:
                        order: 0
                        enabled: true
                        url: ["ldaps://ldap.example.com:636"]
                        bind_dn: "cn=admin,dc=example,dc=com"
                        authorization_realms: "native1"
            """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var config = parseConfig(node);
    var ldap =
        (Realm.LdapRealm)
            config
                .security()
                .get()
                .authc()
                .get()
                .orElseThrow()
                .realms()
                .get()
                .get("ldap")
                .get()
                .get("ldap1")
                .get();
    assertEquals(1, ldap.authorizationRealms().get().size());
    assertEquals("native1", ldap.authorizationRealms().get().get(0).get());
  }

  @Test
  public void testLdapAuthorizationRealmsList() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/elasticsearch/ldap_authorization_realms_list.yml");
    var config = parseConfig(node);
    var ldap =
        (Realm.LdapRealm)
            config
                .security()
                .get()
                .authc()
                .get()
                .orElseThrow()
                .realms()
                .get()
                .get("ldap")
                .get()
                .get("ldapList")
                .get();
    assertEquals(2, ldap.authorizationRealms().get().size());
    assertEquals("file1", ldap.authorizationRealms().get().get(0).get());
    assertEquals("native1", ldap.authorizationRealms().get().get(1).get());
  }

  @Test
  public void testLdapWithoutGroupSearch() throws IOException, ConfigValidationException {
    String yaml =
        """
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
    var config = parseConfig(node);

    var ldap =
        config
            .security()
            .get()
            .authc()
            .get()
            .orElseThrow()
            .realms()
            .get()
            .get("ldap")
            .get()
            .get("ldapNoGroups")
            .get();
    assertNotNull(ldap);
    assertInstanceOf(Realm.LdapRealm.class, ldap);
    var ldapRealm = (Realm.LdapRealm) ldap;
    assertEquals("secret", ldapRealm.bindPassword().get());
    assertTrue(ldapRealm.groupSearchBaseDn().get().isEmpty());
    assertEquals(Realm.SearchScope.SUB_TREE, ldapRealm.groupSearchScope().get()); // Default
    assertTrue(ldapRealm.groupSearchFilter().get().isEmpty());
  }

  @Test
  public void testSecretFieldsAreMarkedAsSecret() throws IOException, ConfigValidationException {
    var config = readConfig("/xpack_migrate/elasticsearch/secret_fields.yml");

    // Verify LDAP realm secrets
    var ldap1 =
        (Realm.LdapRealm)
            config
                .security()
                .get()
                .authc()
                .get()
                .orElseThrow()
                .realms()
                .get()
                .get("ldap")
                .get()
                .get("ldap1")
                .get();
    assertTrue(ldap1.bindPassword().isSecret(), "LDAP bind_password should be secret");
    assertTrue(ldap1.secureBindPassword().isSecret(), "LDAP secure_bind_password should be secret");
    assertTrue(
        ldap1.sslKeystoreSecurePassword().isSecret(),
        "LDAP ssl.keystore.secure_password should be secret");
    assertTrue(
        ldap1.sslKeystoreSecureKeyPassword().isSecret(),
        "LDAP ssl.keystore.secure_key_password should be secret");

    // Verify Active Directory realm secrets
    var ad1 =
        (Realm.ActiveDirectoryRealm)
            config
                .security()
                .get()
                .authc()
                .get()
                .orElseThrow()
                .realms()
                .get()
                .get("active_directory")
                .get()
                .get("ad1")
                .get();
    assertTrue(ad1.bindPassword().isSecret(), "AD bind_password should be secret");
    assertTrue(ad1.secureBindPassword().isSecret(), "AD secure_bind_password should be secret");

    // Verify SAML realm keystore secrets
    var saml1 =
        (Realm.SAMLRealm)
            config
                .security()
                .get()
                .authc()
                .get()
                .orElseThrow()
                .realms()
                .get()
                .get("saml")
                .get()
                .get("saml1")
                .get();
    var signingKeystore = saml1.signingKeystore().get().orElseThrow();
    assertTrue(
        signingKeystore.securePassword().isSecret(),
        "SAML signing keystore secure_password should be secret");
    assertTrue(
        signingKeystore.secureKeyPassword().isSecret(),
        "SAML signing keystore secure_key_password should be secret");
  }

  private XPackElasticsearchConfig readConfig(String path)
      throws IOException, ConfigValidationException {
    return parseConfig(read(path));
  }

  private XPackElasticsearchConfig parseConfig(DocNode node) throws ConfigValidationException {
    var src = new Source.Config("elasticsearch.yml");
    return TraceableDocNode.parse(node, src, XPackElasticsearchConfig::parse);
  }

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return DocNode.wrap(DocReader.yaml().read(in));
    }
  }
}
