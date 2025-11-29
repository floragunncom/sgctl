package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.Objects;

/**
 * Represents X-Pack security configuration from elasticsearch.yml. Focuses on authentication
 * realms, roles, and users
 */
public record XPackElasticsearchConfig(SecurityConfig security) {

  public XPackElasticsearchConfig {
    Objects.requireNonNull(security, "security must not be null");
  }

  /** xpack.security.* */
  public record SecurityConfig(boolean enabled, AuthcConfig authc) {

    public SecurityConfig {
      Objects.requireNonNull(authc, "authc must not be null");
    }

    public static SecurityConfig parse(DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
      var enabled = vDoc.get("enabled").withDefault(true).asBoolean();
      var authc = vDoc.get("authc").by(AuthcConfig::parse);

      vDoc.throwExceptionForPresentErrors();

      return new SecurityConfig(enabled, authc);
    }
  }

  /** xpack.security.authc.* */
  public record AuthcConfig(ImmutableMap<String, Realm> realms) {

    public AuthcConfig {
      Objects.requireNonNull(realms, "realms must not be null");
    }

    public static AuthcConfig parse(DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
      var realms = vDoc.get("realms").by(ctx -> parseRealms(ctx, context));

      vDoc.throwExceptionForPresentErrors();

      return new AuthcConfig(realms);
    }

    private static ImmutableMap<String, Realm> parseRealms(DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var builder = new ImmutableMap.Builder<String, Realm>(doc.size());

      // structured as: xpack.security.authc.realms.<type>.<name>
      for (var realmType : doc.keySet()) {
        var typeNode = doc.getAsNode(realmType);
        for (var realmName : typeNode.keySet()) {
          var realmConfig = typeNode.getAsNode(realmName);
          var realm = Realm.parse(realmType, realmName, realmConfig, context);
          builder.with(realmType + "." + realmName, realm);
        }
      }

      return builder.build();
    }
  }

  /** Base interface for all realm types. Supports: native, file, ldap, active_directory */
  public sealed interface Realm {
    String type();

    String name();

    int order();

    boolean enabled();

    record NativeRealm(String type, String name, int order, boolean enabled) implements Realm {
      public NativeRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    record FileRealm(String type, String name, int order, boolean enabled) implements Realm {
      public FileRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    record LdapRealm(
        String type,
        String name,
        int order,
        boolean enabled,
        ImmutableList<String> url,
        String bindDn,
        String secureBindPassword,
        String userDnTemplates,
        String authorizationRealms,
        String userGroupAttr,
        String userFullNameAttr,
        String userEmailAttr,
        String userSearchBaseDn,
        String userSearchScope,
        String userSearchFilter,
        String groupSearchBaseDn,
        String groupSearchScope,
        String groupSearchFilter,
        boolean unmappedGroupsAsRoles,
        String sslKey,
        String sslSecureKeyPassphrase,
        String sslCertificate,
        ImmutableList<String> sslCertificateAuthorities,
        String sslKeystorePath,
        String sslKeystoreType,
        String sslKeystoreSecurePassword,
        String sslKeystoreSecureKeyPassword)
        implements Realm {
      public LdapRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(url, "url must not be null");
      }
    }

    record ActiveDirectoryRealm(
        String type,
        String name,
        int order,
        boolean enabled,
        String domainName,
        ImmutableList<String> url,
        String bindDn,
        String userSearchBaseDn,
        boolean unmappedGroupsAsRoles)
        implements Realm {
      public ActiveDirectoryRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    /** TODO jwt, saml, oidc, kerberos, pki */
    record GenericRealm(String type, String name, int order, boolean enabled, DocNode rawConfig)
        implements Realm {
      public GenericRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(rawConfig, "rawConfig must not be null");
      }
    }

    static Realm parse(String type, String name, DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
      var order = vDoc.get("order").required().asInt();
      var enabled = vDoc.get("enabled").withDefault(true).asBoolean();

      return switch (type) {
        case "native" -> new NativeRealm(type, name, order, enabled);
        case "file" -> new FileRealm(type, name, order, enabled);
        case "ldap" -> parseLdapRealm(type, name, order, enabled, vDoc);
        case "active_directory" -> parseActiveDirectoryRealm(type, name, order, enabled, vDoc);
        // Stretch goals - store as generic for future implementation
        case "jwt", "saml", "oidc", "kerberos", "pki" ->
            new GenericRealm(type, name, order, enabled, doc);
        default -> new GenericRealm(type, name, order, enabled, doc);
      };
    }

    private static LdapRealm parseLdapRealm(
        String type, String name, int order, boolean enabled, ValidatingDocNode vDoc)
        throws ConfigValidationException {
      var url = vDoc.get("url").asList().ofStrings();
      var bindDn = vDoc.get("bind_dn").asString();
      var secureBindPassword = vDoc.get("secure_bind_password").asString();
      // TODO: Gotta look into whether a list is also possible
      var authorizationRealms = vDoc.get("authorization_realms").withDefault("").asString();
      var userDnTemplates = vDoc.get("user_dn_templates").asString();
      var userGroupAttr = vDoc.get("user_group_attribute").withDefault("memberOf").asString();
      var userFullNameAttr = vDoc.get("user_full_name_attribute").asString();
      var userEmailAttr = vDoc.get("user_email_attribute").asString();

      var userSearchNode = vDoc.get("user_search").asDocNode();
      var userSearchScope =
          Objects.requireNonNullElse(userSearchNode.getAsString("scope"), "sub_tree");
      var userSearchBaseDn = userSearchNode.getAsString("base_dn");
      var userSearchFilter =
          Objects.requireNonNullElse(userSearchNode.getAsString("filter"), "(uid={0})");

      var groupSearchNode = vDoc.get("group_search").asDocNode();
      var groupSearchBaseDn = groupSearchNode.getAsString("base_dn");
      var groupSearchScope = groupSearchNode.getAsString("scope");
      var groupSearchFilter = groupSearchNode.getAsString("filter");

      var unmappedGroupsAsRoles =
          vDoc.get("unmapped_groups_as_roles").withDefault(false).asBoolean();

      var sslNode = vDoc.get("ssl").asDocNode();
      var sslKey = sslNode.getAsString("key");
      var sslSecureKeyPassphrase = sslNode.getAsString("secure_key_passphrase");
      var sslCertificate = sslNode.getAsString("certificate");
      var sslCertificateAuthorities = sslNode.getAsListOfStrings("certificate_authorities");

      var sslKeystoreNode = sslNode.getAsNode("keystore");
      var sslKeystorePath = sslKeystoreNode.getAsString("path");
      var sslKeystoreType = sslKeystoreNode.getAsString("type");
      var sslKeystoreSecurePassword = sslKeystoreNode.getAsString("secure_password");
      var sslKeystoreSecureKeyPassword = sslKeystoreNode.getAsString("secure_key_password");

      vDoc.throwExceptionForPresentErrors();

      return new LdapRealm(
          type,
          name,
          order,
          enabled,
          url,
          bindDn,
          secureBindPassword,
          userDnTemplates,
          authorizationRealms,
          userGroupAttr,
          userFullNameAttr,
          userEmailAttr,
          userSearchBaseDn,
          userSearchScope,
          userSearchFilter,
          groupSearchBaseDn,
          groupSearchScope,
          groupSearchFilter,
          unmappedGroupsAsRoles,
          sslKey,
          sslSecureKeyPassphrase,
          sslCertificate,
          sslCertificateAuthorities,
          sslKeystorePath,
          sslKeystoreType,
          sslKeystoreSecurePassword,
          sslKeystoreSecureKeyPassword);
    }

    private static ActiveDirectoryRealm parseActiveDirectoryRealm(
        String type, String name, int order, boolean enabled, ValidatingDocNode vDoc)
        throws ConfigValidationException {
      var domainName = vDoc.get("domain_name").asString();
      var url = vDoc.get("url").asList().ofStrings();
      var bindDn = vDoc.get("bind_dn").asString();
      var userSearchNode = vDoc.get("user_search").asDocNode();
      var userSearchBaseDn = userSearchNode != null ? userSearchNode.getAsString("base_dn") : null;
      var unmappedGroupsAsRoles =
          vDoc.get("unmapped_groups_as_roles").withDefault(false).asBoolean();

      vDoc.throwExceptionForPresentErrors();

      return new ActiveDirectoryRealm(
          type,
          name,
          order,
          enabled,
          domainName,
          url,
          bindDn,
          userSearchBaseDn,
          unmappedGroupsAsRoles);
    }
  }

  /**
   * Parses X-Pack elasticsearch.yml security configuration. Expects the full elasticsearch.yml
   * content, extracts xpack.security.* section.
   *
   * @param doc The complete elasticsearch.yml as DocNode
   * @param context Parser context
   * @return Parsed XPackElasticsearchConfig
   * @throws ConfigValidationException If validation fails
   */
  public static XPackElasticsearchConfig parse(DocNode doc, Parser.Context context)
      throws ConfigValidationException {
    var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
    var xpackNode = vDoc.get("xpack").required().asDocNode();
    var security =
        new ValidatingDocNode(xpackNode, new ValidationErrors(), context)
            .get("security")
            .by(SecurityConfig::parse);

    vDoc.throwExceptionForPresentErrors();

    return new XPackElasticsearchConfig(security);
  }
}
