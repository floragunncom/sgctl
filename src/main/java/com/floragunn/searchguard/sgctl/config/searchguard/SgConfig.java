package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// Technically legacy, but can use MigrateConfig to convert to new config files
public record SgConfig(ImmutableMap<String, Object> sgMeta, SearchGuard searchGuard)
    implements NamedConfig<SgConfig> {

  public SgConfig {
    if (sgMeta == null) {
      sgMeta = ImmutableMap.empty();
    }
    Objects.requireNonNull(searchGuard, "searchGuard must not be null");
  }

  // Convenience factory for configs without a {@code _sg_meta} section
  public static SgConfig ofSearchGuardConfig(SearchGuard searchGuard) {
    return new SgConfig(ImmutableMap.empty(), searchGuard);
  }

  @Override
  public String getFileName() {
    return "sg_config.yml";
  }

  @Override
  public Object toBasicObject() {
    Map<String, Object> root = new LinkedHashMap<>();
    if (!sgMeta.isEmpty()) {
      root.put("_sg_meta", sgMeta);
    }
    root.put("_searchguard", searchGuard.toBasicObject());
    return root;
  }

  public record SearchGuard(Boolean xpackMigrationMode, String license, Dynamic dynamic) {

    public SearchGuard {
      Objects.requireNonNull(dynamic, "dynamic must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (xpackMigrationMode != null) {
        result.put("xpack_migration_mode", xpackMigrationMode);
      }
      if (license != null) {
        result.put("license", license);
      }
      result.put("dynamic", dynamic.toBasicObject());
      return result;
    }
  }

  public record Dynamic(
      String filteredAliasMode,
      Boolean multiRolespanEnabled,
      String hostsResolverMode,
      Boolean doNotFailOnForbidden,
      Boolean doNotFailOnForbiddenEmpty,
      Kibana kibana,
      Http http,
      AuthTokenProvider authTokenProvider,
      ImmutableMap<String, AuthcDomain> authc,
      ImmutableMap<String, AuthzDomain> authz) {

    public Dynamic {
      authc = authc == null ? ImmutableMap.empty() : authc;
      authz = authz == null ? ImmutableMap.empty() : authz;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (filteredAliasMode != null) {
        result.put("filtered_alias_mode", filteredAliasMode);
      }
      if (multiRolespanEnabled != null) {
        result.put("multi_rolespan_enabled", multiRolespanEnabled);
      }
      if (hostsResolverMode != null) {
        result.put("hosts_resolver_mode", hostsResolverMode);
      }
      if (doNotFailOnForbidden != null) {
        result.put("do_not_fail_on_forbidden", doNotFailOnForbidden);
      }
      if (doNotFailOnForbiddenEmpty != null) {
        result.put("do_not_fail_on_forbidden_empty", doNotFailOnForbiddenEmpty);
      }
      if (kibana != null) {
        result.put("kibana", kibana.toBasicObject());
      }
      if (http != null) {
        result.put("http", http.toBasicObject());
      }
      if (authTokenProvider != null) {
        result.put("auth_token_provider", authTokenProvider.toBasicObject());
      }
      if (!authc.isEmpty()) {
        Map<String, Object> authcMap = new LinkedHashMap<>();
        authc.forEach((key, value) -> authcMap.put(key, value.toBasicObject()));
        result.put("authc", authcMap);
      }
      if (!authz.isEmpty()) {
        Map<String, Object> authzMap = new LinkedHashMap<>();
        authz.forEach((key, value) -> authzMap.put(key, value.toBasicObject()));
        result.put("authz", authzMap);
      }
      return result;
    }
  }

  public record Kibana(Boolean multitenancyEnabled, String serverUsername, String index) {
    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (multitenancyEnabled != null) {
        result.put("multitenancy_enabled", multitenancyEnabled);
      }
      if (serverUsername != null) {
        result.put("server_username", serverUsername);
      }
      if (index != null) {
        result.put("index", index);
      }
      return result;
    }
  }

  public record Http(Boolean anonymousAuthEnabled, Xff xff) {
    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (anonymousAuthEnabled != null) {
        result.put("anonymous_auth_enabled", anonymousAuthEnabled);
      }
      if (xff != null) {
        result.put("xff", xff.toBasicObject());
      }
      return result;
    }
  }

  public record Xff(Boolean enabled, String remoteIpHeader, String internalProxies) {
    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (enabled != null) {
        result.put("enabled", enabled);
      }
      if (remoteIpHeader != null) {
        result.put("remoteIpHeader", remoteIpHeader);
      }
      if (internalProxies != null) {
        result.put("internalProxies", internalProxies);
      }
      return result;
    }
  }

  public record AuthTokenProvider(
      Boolean enabled, String name, ImmutableMap<String, Boolean> users) {

    public AuthTokenProvider {
      users = users == null ? ImmutableMap.empty() : users;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (enabled != null) {
        result.put("enabled", enabled);
      }
      if (name != null) {
        result.put("name", name);
      }
      if (!users.isEmpty()) {
        result.put("users", users);
      }
      return result;
    }
  }

  public record AuthcDomain(
      boolean httpEnabled,
      boolean transportEnabled,
      int order,
      HttpAuthenticator httpAuthenticator,
      AuthenticationBackend authenticationBackend,
      AuthorizationBackend authorizationBackend) {

    public AuthcDomain {
      Objects.requireNonNull(httpAuthenticator, "httpAuthenticator must not be null");
      Objects.requireNonNull(authenticationBackend, "authenticationBackend must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("http_enabled", httpEnabled);
      result.put("transport_enabled", transportEnabled);
      result.put("order", order);
      result.put("http_authenticator", httpAuthenticator.toBasicObject());
      result.put("authentication_backend", authenticationBackend.toBasicObject());
      if (authorizationBackend != null) {
        result.put("authorization_backend", authorizationBackend.toBasicObject());
      }
      return result;
    }
  }

  public record AuthzDomain(
      boolean httpEnabled, boolean transportEnabled, AuthorizationBackend authorizationBackend) {

    public AuthzDomain {
      Objects.requireNonNull(authorizationBackend, "authorizationBackend must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("http_enabled", httpEnabled);
      result.put("transport_enabled", transportEnabled);
      result.put("authorization_backend", authorizationBackend.toBasicObject());
      return result;
    }
  }

  public interface HttpAuthenticator {

    Map<String, Object> toBasicObject();

    boolean challenge();

    record Basic(boolean challenge) implements HttpAuthenticator {

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "basic");
        result.put("challenge", challenge);
        return result;
      }
    }

    record Kerberos(boolean challenge, boolean krbDebug, boolean stripRealmFromPrincipal)
        implements HttpAuthenticator {

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "kerberos");
        result.put("challenge", challenge);
        result.put("krb_debug", krbDebug);
        result.put("strip_realm_from_principal", stripRealmFromPrincipal);
        return result;
      }
    }

    record Jwt(
        boolean challenge,
        String signingKey,
        String jwtHeader, // optional, defaults to Authorization
        String jwtUrlParameter,
        String subjectKey, // optional, exclusive with subjectPath
        String subjectPath, // optional, exclusive with subjectKey
        String rolesKey, // optional, exclusive with rolesPath
        String rolesPath, // optional, exclusive with rolesKey
        String subjectPattern // optional
        ) implements HttpAuthenticator {

      public Jwt {
        Objects.requireNonNull(signingKey, "signingKey must not be null");
        Objects.requireNonNull(jwtUrlParameter, "jwtUrlParameter must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "jwt");
        result.put("challenge", challenge);
        result.put("signing_key", signingKey);
        if (jwtHeader != null) {
          result.put("jwt_header", jwtHeader);
        }
        result.put("jwt_url_parameter", jwtUrlParameter);
        if (subjectKey != null) {
          result.put("subject_key", subjectKey);
        }
        if (subjectPath != null) {
          result.put("subject_path", subjectPath);
        }
        if (rolesKey != null) {
          result.put("roles_key", rolesKey);
        }
        if (rolesPath != null) {
          result.put("roles_path", rolesPath);
        }
        if (subjectPattern != null) {
          result.put("subject_pattern", subjectPattern);
        }
        return result;
      }
    }

    record Openid(
        boolean challenge,
        String openidConnectUrl,
        String jwtHeader, // optional, defaults to Authorization
        String jwtUrlParameter, // optional
        String subjectKey, // optional
        String rolesKey, // optional
        String subjectPattern, // optional
        String proxy // optional
        ) implements HttpAuthenticator {

      public Openid {
        Objects.requireNonNull(openidConnectUrl, "openidConnectUrl must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "openid");
        result.put("challenge", challenge);
        result.put("openid_connect_url", openidConnectUrl);
        if (jwtHeader != null) {
          result.put("jwt_header", jwtHeader);
        }
        if (jwtUrlParameter != null) {
          result.put("jwt_url_parameter", jwtUrlParameter);
        }
        if (subjectKey != null) {
          result.put("subject_key", subjectKey);
        }
        if (rolesKey != null) {
          result.put("roles_key", rolesKey);
        }
        if (subjectPattern != null) {
          result.put("subject_pattern", subjectPattern);
        }
        if (proxy != null) {
          result.put("proxy", proxy);
        }
        return result;
      }
    }

    // TODO: saml (stretch goal)

    record Clientcert(boolean challenge, String usernameAttribute) implements HttpAuthenticator {
      public Clientcert {
        Objects.requireNonNull(usernameAttribute, "usernameAttribute must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "clientcert");
        result.put("challenge", challenge);

        Map<String, Object> config = new LinkedHashMap<>();
        if (usernameAttribute != null) {
          config.put("username_attribute", usernameAttribute);
        }
        result.put("config", config);
        return result;
      }
    }

    record Proxy(boolean challenge, String userHeader, String rolesHeader, String rolesSeparator)
        implements HttpAuthenticator {

      public Proxy {
        Objects.requireNonNull(userHeader, "userHeader must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "proxy");
        result.put("challenge", challenge);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("user_header", userHeader);
        if (rolesHeader != null) {
          config.put("roles_header", rolesHeader);
        }
        if (rolesSeparator != null) {
          config.put("roles_separator", rolesSeparator);
        }
        if (!config.isEmpty()) {
          result.put("config", config);
        }
        return result;
      }
    }
  }

  public record Authenticator(Type type, boolean challenge, ImmutableMap<String, Object> config) {

    public Authenticator {
      Objects.requireNonNull(type, "type must not be null");
      config = config == null ? ImmutableMap.empty() : config;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("type", type.name().toLowerCase(Locale.ROOT));
      result.put("challenge", challenge);
      if (!config.isEmpty()) {
        result.put("config", config);
      }
      return result;
    }

    public enum Type {
      BASIC,
      KERBEROS,
      JWT,
      OPENID,
      SAML,
      PROXY,
      CLIENTCERT
    }
  }

  public interface AuthenticationBackend {

    Map<String, Object> toBasicObject();

    record Noop() implements AuthenticationBackend {

      @Override
      public Map<String, Object> toBasicObject() {
        return Map.of("type", "noop");
      }
    }

    record Internal() implements AuthenticationBackend {

      @Override
      public Map<String, Object> toBasicObject() {
        return Map.of("type", "internal");
      }
    }

    record Ldap(
        boolean enableSsl,
        boolean enableStartTls,
        boolean enableSslClientAuth,
        boolean verifyHostnames,
        ImmutableList<String> hosts,
        String bindDn,
        String password,
        String userbase,
        String usersearch,
        String usernameAttribute)
        implements AuthenticationBackend {

      public Ldap {
        Objects.requireNonNull(hosts, "hosts must not be null");
        Objects.requireNonNull(bindDn, "bindDn must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(userbase, "userbase must not be null");
        Objects.requireNonNull(usersearch, "usersearch must not be null");
        Objects.requireNonNull(usernameAttribute, "usernameAttribute must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "ldap");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enable_ssl", enableSsl);
        config.put("enable_start_tls", enableStartTls);
        config.put("enable_ssl_client_auth", enableSslClientAuth);
        config.put("verify_hostnames", verifyHostnames);
        config.put("hosts", hosts);
        config.put("bind_dn", bindDn);
        config.put("password", password);
        config.put("userbase", userbase);
        config.put("usersearch", usersearch);
        config.put("username_attribute", usernameAttribute);
        result.put("config", config);
        return result;
      }
    }
  }

  public interface AuthorizationBackend {

    Map<String, Object> toBasicObject();

    record Noop() implements AuthorizationBackend {

      @Override
      public Map<String, Object> toBasicObject() {
        return Map.of("type", "noop");
      }
    }

    record Ldap(
        boolean enableSsl,
        boolean enableStartTls,
        boolean enableSslClientAuth,
        boolean verifyHostnames,
        ImmutableList<String> hosts,
        String bindDn,
        String password,
        String userbase,
        String usersearch,
        String usernameAttribute,
        String rolebase,
        String rolesearch,
        String userroleattribute,
        String userrolename,
        String rolename,
        boolean resolveNestedRoles,
        ImmutableList<String> skipUsers)
        implements AuthorizationBackend {

      public Ldap {
        Objects.requireNonNull(hosts, "hosts must not be null");
        Objects.requireNonNull(bindDn, "bindDn must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(userbase, "userbase must not be null");
        Objects.requireNonNull(usersearch, "usersearch must not be null");
        Objects.requireNonNull(usernameAttribute, "usernameAttribute must not be null");
        Objects.requireNonNull(rolebase, "rolebase must not be null");
        Objects.requireNonNull(rolesearch, "rolesearch must not be null");
        Objects.requireNonNull(userroleattribute, "userroleattribute must not be null");
        Objects.requireNonNull(userrolename, "userrolename must not be null");
        Objects.requireNonNull(rolename, "rolename must not be null");
        Objects.requireNonNull(skipUsers, "skipUsers must not be null");
      }

      @Override
      public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "ldap");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enable_ssl", enableSsl);
        config.put("enable_start_tls", enableStartTls);
        config.put("enable_ssl_client_auth", enableSslClientAuth);
        config.put("verify_hostnames", verifyHostnames);
        config.put("hosts", hosts);
        config.put("bind_dn", bindDn);
        config.put("password", password);
        config.put("userbase", userbase);
        config.put("usersearch", usersearch);
        config.put("username_attribute", usernameAttribute);
        config.put("rolebase", rolebase);
        config.put("rolesearch", rolesearch);
        config.put("userroleattribute", userroleattribute);
        config.put("rolename", rolename);
        config.put("resolve_nested_roles", resolveNestedRoles);
        config.put("skip_users", skipUsers);
        result.put("config", config);
        return result;
      }
    }
  }
}
