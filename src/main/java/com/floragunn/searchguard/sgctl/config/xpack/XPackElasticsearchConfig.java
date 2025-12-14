package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.*;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Represents X-Pack security configuration from elasticsearch.yml. Focuses on authentication
 * realms, roles, and users
 */
@NullMarked
public record XPackElasticsearchConfig(Traceable<SecurityConfig> security) {

  public XPackElasticsearchConfig {
    Objects.requireNonNull(security, "security must not be null");
  }

  /** xpack.security.* */
  public record SecurityConfig(Traceable<Boolean> enabled, Traceable<AuthcConfig> authc) {

    public SecurityConfig {
      Objects.requireNonNull(authc, "authc must not be null");
    }

    public static SecurityConfig parse(TraceableDocNode tDoc) throws ConfigValidationException {
      var enabled = tDoc.get("enabled").asBoolean(true);
      var authc = tDoc.get("authc").as(AuthcConfig::parse);

      return new SecurityConfig(enabled, authc);
    }
  }

  /** xpack.security.authc.* */
  public record AuthcConfig(
      OptTraceable<ImmutableMap<String, Traceable<ImmutableMap<String, Traceable<Realm>>>>>
          realms) {

    public AuthcConfig {
      Objects.requireNonNull(realms, "realms must not be null");
    }

    private static Traceable<ImmutableMap<String, Traceable<Realm>>> parseRealmType(
        TraceableAttribute.Required tAttr) {
      var type = Traceable.of(tAttr.getSource(), tAttr.getSource().pathPart());
      return tAttr.asMapOf(
          (TraceableDocNodeParser<Realm>)
              (realmTDoc) -> {
                var name = Traceable.of(realmTDoc.getSource(), realmTDoc.getSource().pathPart());
                return Realm.parse(type, name, realmTDoc);
              });
    }

    public static AuthcConfig parse(TraceableDocNode tDoc) throws ConfigValidationException {
      var realms = tDoc.get("realms").asMapOf(AuthcConfig::parseRealmType);

      return new AuthcConfig(realms);
    }
  }

  /** Base interface for all realm types. Supports: native, file, ldap, active_directory */
  public sealed interface Realm {
    Traceable<String> type();

    Traceable<String> name();

    Traceable<Integer> order();

    Traceable<Boolean> enabled();

    record NativeRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled)
        implements Realm {
      public NativeRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    record FileRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled)
        implements Realm {
      public FileRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    record LdapRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        Traceable<ImmutableList<Traceable<String>>> url,
        Traceable<String> bindDn,
        Traceable<String> bindPassword,
        Traceable<String> secureBindPassword,
        Traceable<String> userDnTemplates,
        Traceable<ImmutableList<Traceable<String>>> authorizationRealms,
        Traceable<String> userGroupAttr,
        Traceable<String> userFullNameAttr,
        Traceable<String> userEmailAttr,
        Traceable<String> userSearchBaseDn,
        Traceable<Scope> userSearchScope,
        Traceable<String> userSearchFilter,
        Traceable<String> groupSearchBaseDn,
        Traceable<Scope> groupSearchScope,
        Traceable<String> groupSearchFilter,
        Traceable<Boolean> unmappedGroupsAsRoles,
        Traceable<String> sslKey,
        Traceable<String> sslSecureKeyPassphrase,
        Traceable<String> sslCertificate,
        Traceable<ImmutableList<Traceable<String>>> sslCertificateAuthorities,
        Traceable<String> sslKeystorePath,
        Traceable<String> sslKeystoreType,
        Traceable<String> sslKeystoreSecurePassword,
        Traceable<String> sslKeystoreSecureKeyPassword)
        implements Realm {
      public enum Scope {
        SUB_TREE,
        ONE_LEVEL,
        BASE
      }

      public LdapRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(url, "url must not be null");
      }
    }

    record ActiveDirectoryRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        Traceable<String> domainName,
        Traceable<ImmutableList<Traceable<String>>> url,
        Traceable<String> bindDn,
        Traceable<String> userSearchBaseDn,
        Traceable<Boolean> unmappedGroupsAsRoles)
        implements Realm {
      public ActiveDirectoryRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    /** TODO jwt, saml, oidc, kerberos, pki */
    record GenericRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        Traceable<DocNode> rawConfig)
        implements Realm {
      public GenericRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(rawConfig, "rawConfig must not be null");
      }
    }

    static Realm parse(Traceable<String> tType, Traceable<String> name, TraceableDocNode tDoc)
        throws ConfigValidationException {
      var order = tDoc.get("order").required().asInt();
      var enabled = tDoc.get("enabled").asBoolean(true);

      var type = tType.get(); // Is ok, because created
      return switch (type) {
        case "native" -> new NativeRealm(tType, name, order, enabled);
        case "file" -> new FileRealm(tType, name, order, enabled);
        case "ldap" -> parseLdapRealm(tType, name, order, enabled, tDoc, vDoc);
        case "active_directory" -> parseActiveDirectoryRealm(tType, name, order, enabled, vDoc);
        // Stretch goals - store as generic for future implementation
        case "jwt", "saml", "oidc", "kerberos", "pki" ->
            new GenericRealm(tType, name, order, enabled, tDoc);
        default -> new GenericRealm(tType, name, order, enabled, tDoc);
      };
    }

    private static LdapRealm parseLdapRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        TraceableDocNode tDoc)
        throws ConfigValidationException {
      var url = tDoc.get("url").asListOfStrings(ImmutableList.empty());
      var bindDn = tDoc.get("bind_dn").asString("");
      var bindPassword = tDoc.get("bind_password").asString("");
      var secureBindPassword = tDoc.get("secure_bind_password").asString("");
      // authorization realms can be a string or list; normalize to a list
      // TODO: authorization_realms can be a List or just a String. Only the List case is handled
      // here.
      var authorizationRealms =
          tDoc.get("authorization_realms").asListOfStrings(ImmutableList.empty());
      var userDnTemplates = tDoc.get("user_dn_templates").asString();
      var userGroupAttr = tDoc.get("user_group_attribute").asString("memberOf");
      var userFullNameAttr = tDoc.get("user_full_name_attribute").asString("cn");
      var userEmailAttr = tDoc.get("user_email_attribute").asString("mail");

      // TODO: Use default value "sub_tree" for enum
      var userSearchScopeString = tDoc.get("user_search.scope").asEnum(LdapRealm.Scope.class);
      //      var userSearchScope = LdapRealm.Scope.valueOf(userSearchScopeString.);
      var userSearchBaseDn = userSearchNode.getAsString("base_dn");
      var userSearchFilter =
          Objects.requireNonNullElse(userSearchNode.getAsString("filter"), "(uid={0})");

      var groupSearchNode = tDoc.get("group_search").asDocNode();
      var groupSearchBaseDn = groupSearchNode.getAsString("base_dn");
      var groupSearchScopeString = groupSearchNode.getAsString("scope");
      var groupSearchScope =
          groupSearchScopeString != null
              ? LdapRealm.Scope.valueOf(groupSearchScopeString.toUpperCase(Locale.ROOT))
              : null;
      var groupSearchFilter = groupSearchNode.getAsString("filter");

      var unmappedGroupsAsRoles =
          tDoc.get("unmapped_groups_as_roles").withDefault(false).asBoolean();

      var sslNode = tDoc.get("ssl").asDocNode();
      var sslKey = sslNode.getAsString("key");
      var sslSecureKeyPassphrase = sslNode.getAsString("secure_key_passphrase");
      var sslCertificate = sslNode.getAsString("certificate");
      var sslCertificateAuthorities = sslNode.getAsListOfStrings("certificate_authorities");

      var sslKeystoreNode = sslNode.getAsNode("keystore");
      var sslKeystorePath = sslKeystoreNode.getAsString("path");
      var sslKeystoreType = sslKeystoreNode.getAsString("type");
      var sslKeystoreSecurePassword = sslKeystoreNode.getAsString("secure_password");
      var sslKeystoreSecureKeyPassword = sslKeystoreNode.getAsString("secure_key_password");

      tDoc.throwExceptionForPresentErrors();

      return new LdapRealm(
          type,
          name,
          order,
          enabled,
          url,
          bindDn,
          bindPassword,
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
  public static XPackElasticsearchConfig parse(DocNode doc, Parser.Context _context)
      throws ConfigValidationException {
    var errors = new ValidationErrors();
    var tDoc = TraceableDocNode.of(doc, new Source.Config("elasticsearch.yml"), errors);
    var security = tDoc.get("xpack.security").required().as(SecurityConfig::parse);

    errors.throwExceptionForPresentErrors();

    return new XPackElasticsearchConfig(security);
  }
}
