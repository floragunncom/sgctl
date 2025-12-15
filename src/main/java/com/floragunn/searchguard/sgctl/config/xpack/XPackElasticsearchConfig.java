package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.*;
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
  public record SecurityConfig(Traceable<Boolean> enabled, OptTraceable<AuthcConfig> authc) {

    public SecurityConfig {
      Objects.requireNonNull(authc, "authc must not be null");
    }

    public static SecurityConfig parse(TraceableDocNode tDoc) {
      var enabled = tDoc.get("enabled").asBoolean(true);
      var authc = tDoc.get("authc").as(AuthcConfig::parse);

      return new SecurityConfig(enabled, authc);
    }
  }

  /** xpack.security.authc.* */
  public record AuthcConfig(
      Traceable<ImmutableMap<String, Traceable<ImmutableMap<String, Traceable<Realm>>>>> realms) {

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

    public static AuthcConfig parse(TraceableDocNode tDoc) {
      var realms = tDoc.get("realms").asMapOf(AuthcConfig::parseRealmType, ImmutableMap.empty());

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
        OptTraceable<String> bindDn,
        OptTraceable<String> bindPassword,
        OptTraceable<String> secureBindPassword,
        OptTraceable<String> userDnTemplates,
        Traceable<ImmutableList<Traceable<String>>> authorizationRealms,
        Traceable<String> userGroupAttr,
        Traceable<String> userFullNameAttr,
        Traceable<String> userEmailAttr,
        OptTraceable<String> userSearchBaseDn,
        Traceable<Scope> userSearchScope,
        Traceable<String> userSearchFilter,
        OptTraceable<String> groupSearchBaseDn,
        Traceable<Scope> groupSearchScope,
        OptTraceable<String> groupSearchFilter,
        Traceable<Boolean> unmappedGroupsAsRoles,
        OptTraceable<String> sslKey,
        OptTraceable<String> sslSecureKeyPassphrase,
        OptTraceable<String> sslCertificate,
        OptTraceable<ImmutableList<Traceable<String>>> sslCertificateAuthorities,
        OptTraceable<String> sslKeystorePath,
        OptTraceable<String> sslKeystoreType,
        OptTraceable<String> sslKeystoreSecurePassword,
        OptTraceable<String> sslKeystoreSecureKeyPassword)
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
        OptTraceable<ImmutableList<Traceable<String>>> url,
        OptTraceable<String> bindDn,
        OptTraceable<String> userSearchBaseDn,
        Traceable<Boolean> unmappedGroupsAsRoles)
        implements Realm {
      public ActiveDirectoryRealm {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
      }
    }

    record SAMLRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        OptTraceable<String> idpEntityId,
        OptTraceable<String> idpMetadataPath,
        Traceable<Boolean> idpMetadataHttpFailOnError,
        Traceable<String> idpMetadataHttpConnectTimeout,
        Traceable<String> idpMetadataHttpReadTimeout,
        Traceable<String> idpMetadataHttpRefresh,
        Traceable<String> idpMetadataHttpMinimumRefresh,
        Traceable<Boolean> idpUseSingleLogout,
        OptTraceable<String> spEntityId,
        OptTraceable<String> spAcs,
        OptTraceable<String> spLogout)
        implements Realm {
      public SAMLRealm {
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

    static Realm parse(Traceable<String> tType, Traceable<String> name, TraceableDocNode tDoc) {
      var order = tDoc.get("order").required().asInt();
      var enabled = tDoc.get("enabled").asBoolean(true);

      var type = tType.get(); // Is ok, because created
      return switch (type) {
        case "native" -> new NativeRealm(tType, name, order, enabled);
        case "file" -> new FileRealm(tType, name, order, enabled);
        case "ldap" -> parseLdapRealm(tType, name, order, enabled, tDoc);
        case "active_directory" -> parseActiveDirectoryRealm(tType, name, order, enabled, tDoc);
        // Stretch goals - store as generic for future implementation
        case "jwt", "saml", "oidc", "kerberos", "pki" ->
            new GenericRealm(tType, name, order, enabled, tDoc.asAttribute().asDocNode());
        default -> new GenericRealm(tType, name, order, enabled, tDoc.asAttribute().asDocNode());
      };
    }

    private static LdapRealm parseLdapRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        TraceableDocNode tDoc) {
      var url = tDoc.get("url").asListOfStrings(ImmutableList.empty());
      var bindDn = tDoc.get("bind_dn").asString();
      var bindPassword = tDoc.get("bind_password").asString();
      var secureBindPassword = tDoc.get("secure_bind_password").asString();
      // authorization realms can be a string or list; normalize to a list
      // TODO: authorization_realms can be a List or just a String. Only the List case is handled
      // here.
      var authorizationRealms =
          tDoc.get("authorization_realms").asListOfStrings(ImmutableList.empty());
      var userDnTemplates = tDoc.get("user_dn_templates").asString();
      var userGroupAttr = tDoc.get("user_group_attribute").asString("memberOf");
      var userFullNameAttr = tDoc.get("user_full_name_attribute").asString("cn");
      var userEmailAttr = tDoc.get("user_email_attribute").asString("mail");

      var userSearchScope =
          tDoc.get("user_search.scope").asEnum(LdapRealm.Scope.class, LdapRealm.Scope.SUB_TREE);
      var userSearchBaseDn = tDoc.get("user_search.base_dn").asString();
      var userSearchFilter = tDoc.get("user_search.filter").asString("(uid={0})");

      var groupSearchBaseDn = tDoc.get("group_search.base_dn").asString();
      var groupSearchScope =
          tDoc.get("group_search.scope").asEnum(LdapRealm.Scope.class, LdapRealm.Scope.SUB_TREE);
      var groupSearchFilter = tDoc.get("group_search.filter").asString();

      var unmappedGroupsAsRoles = tDoc.get("unmapped_groups_as_roles").asBoolean(false);

      var sslKey = tDoc.get("ssl.key").asString();
      var sslSecureKeyPassphrase = tDoc.get("ssl.secure_key_passphrase").asString();
      var sslCertificate = tDoc.get("ssl.certificate").asString();
      var sslCertificateAuthorities = tDoc.get("ssl.certificate_authorities").asListOfStrings();

      var sslKeystorePath = tDoc.get("ssl.keystore.path").asString();
      var sslKeystoreType = tDoc.get("ssl.keystore.type").asString();
      var sslKeystoreSecurePassword = tDoc.get("ssl.keystore.secure_password").asString();
      var sslKeystoreSecureKeyPassword = tDoc.get("ssl.keystore.secure_key_password").asString();

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

    private static SAMLRealm parseSAMLRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        TraceableDocNode tDoc) {
      var idpEntityId = tDoc.get("idp.entity_id").asString();
      var idpMetadataPath = tDoc.get("idp.metadata.path").asString();
      var idpMetadataHttpFailOnError = tDoc.get("idp.metadata.http.fail_on_error").asBoolean(false);
      var idpMetadataHttpConnectTimeout =
          tDoc.get("idp.metadata.http.connect_timeout").asString("5s");
      var idpMetadataHttpReadTimeout = tDoc.get("idp.metadata.http.read_timeout").asString("10s");
      var idpMetadataHttpRefresh = tDoc.get("idp.metadata.http.refresh").asString("1h");
      var idpMetadataHttpMinimumRefresh =
          tDoc.get("idp.metadata.http.minimum_refresh").asString("5m");
      var idpUseSingleLogout = tDoc.get("idp.use_single_logout").asBoolean(true);

      var spEntityId = tDoc.get("sp.entity_id").asString();
      var spAcs = tDoc.get("sp.acs").asString();
      var spLogout = tDoc.get("sp.logout").asString();
      return new SAMLRealm(
          type,
          name,
          order,
          enabled,
          idpEntityId,
          idpMetadataPath,
          idpMetadataHttpFailOnError,
          idpMetadataHttpConnectTimeout,
          idpMetadataHttpReadTimeout,
          idpMetadataHttpRefresh,
          idpMetadataHttpMinimumRefresh,
          idpUseSingleLogout,
          spEntityId,
          spAcs,
          spLogout);
    }

    private static ActiveDirectoryRealm parseActiveDirectoryRealm(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        TraceableDocNode tDoc) {
      var domainName = tDoc.get("domain_name").required().asString();
      var url = tDoc.get("url").asListOfStrings();
      var bindDn = tDoc.get("bind_dn").asString();
      var userSearchBaseDn = tDoc.get("user_search.base_dn").asString();
      var unmappedGroupsAsRoles = tDoc.get("unmapped_groups_as_roles").asBoolean(false);

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
   * @param _context Parser context
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
