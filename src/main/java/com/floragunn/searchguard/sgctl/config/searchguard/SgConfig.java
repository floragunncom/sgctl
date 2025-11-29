package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.*;

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

  public record SearchGuard(
      Optional<Boolean> xpackMigrationMode, Optional<String> license, Dynamic dynamic) {

    public SearchGuard {
      Objects.requireNonNull(xpackMigrationMode, "xpackMigrationMode must not be null");
      Objects.requireNonNull(license, "license must not be null");
      Objects.requireNonNull(dynamic, "dynamic must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (xpackMigrationMode.isPresent()) {
        result.put("xpack_migration_mode", xpackMigrationMode.get());
      }
      if (license.isPresent()) {
        result.put("license", license.get());
      }
      result.put("dynamic", dynamic.toBasicObject());
      return result;
    }
  }

  public record Dynamic(
      Optional<String> filteredAliasMode,
      Optional<Boolean> multiRolespanEnabled,
      Optional<String> hostsResolverMode,
      Optional<Boolean> doNotFailOnForbidden,
      Optional<Boolean> doNotFailOnForbiddenEmpty,
      Optional<Kibana> kibana,
      Optional<Http> http,
      Optional<AuthTokenProvider> authTokenProvider,
      ImmutableMap<String, AuthcDomain> authc,
      ImmutableMap<String, AuthzDomain> authz) {

    public Dynamic {
      Objects.requireNonNull(filteredAliasMode, "filteredAliasMode must not be null");
      Objects.requireNonNull(multiRolespanEnabled, "multiRolespanEnabled must not be null");
      Objects.requireNonNull(hostsResolverMode, "hostsResolverMode must not be null");
      Objects.requireNonNull(doNotFailOnForbidden, "doNotFailOnForbidden must be null");
      Objects.requireNonNull(doNotFailOnForbiddenEmpty, "doNotFailOnForbiddenEmpty must be null");
      Objects.requireNonNull(kibana, "kibana must not be null");
      Objects.requireNonNull(http, "http must not be null");

      authc = authc == null ? ImmutableMap.empty() : authc;
      authz = authz == null ? ImmutableMap.empty() : authz;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (filteredAliasMode.isPresent()) {
        result.put("filtered_alias_mode", filteredAliasMode.get());
      }
      if (multiRolespanEnabled.isPresent()) {
        result.put("multi_rolespan_enabled", multiRolespanEnabled.get());
      }
      if (hostsResolverMode.isPresent()) {
        result.put("hosts_resolver_mode", hostsResolverMode.get());
      }
      if (doNotFailOnForbidden.isPresent()) {
        result.put("do_not_fail_on_forbidden", doNotFailOnForbidden.get());
      }
      if (doNotFailOnForbiddenEmpty.isPresent()) {
        result.put("do_not_fail_on_forbidden_empty", doNotFailOnForbiddenEmpty.get());
      }
      if (kibana.isPresent()) {
        result.put("kibana", kibana.get().toBasicObject());
      }
      if (http.isPresent()) {
        result.put("http", http.get().toBasicObject());
      }
      if (authTokenProvider.isPresent()) {
        result.put("auth_token_provider", authTokenProvider.get().toBasicObject());
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

  public record Kibana(Optional<Boolean> multitenancyEnabled, String serverUsername, String index) {

    public Kibana {
      Objects.requireNonNull(multitenancyEnabled, "multitenancyEnabled must not be null");
      Objects.requireNonNull(serverUsername, "serverUsername must not be null");
      Objects.requireNonNull(index, "index must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (multitenancyEnabled.isPresent()) {
        result.put("multitenancy_enabled", multitenancyEnabled.get());
      }
      result.put("server_username", serverUsername);
      result.put("index", index);
      return result;
    }
  }

  public record Http(Optional<Boolean> anonymousAuthEnabled, Optional<Xff> xff) {

    public Http {
      Objects.requireNonNull(anonymousAuthEnabled, "anonymousAuthEnabled must not be null");
      Objects.requireNonNull(xff, "xff must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (anonymousAuthEnabled.isPresent()) {
        result.put("anonymous_auth_enabled", anonymousAuthEnabled.get());
      }
      if (xff.isPresent()) {
        result.put("xff", xff.get().toBasicObject());
      }
      return result;
    }
  }

  public record Xff(
      boolean enabled, Optional<String> remoteIpHeader, Optional<String> internalProxies) {

    public Xff {
      Objects.requireNonNull(remoteIpHeader, "remoteIpHeader must not be null");
      Objects.requireNonNull(internalProxies, "internalProxies must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("enabled", enabled);
      if (remoteIpHeader.isPresent()) {
        result.put("remoteIpHeader", remoteIpHeader.get());
      }
      if (internalProxies.isPresent()) {
        result.put("internalProxies", internalProxies.get());
      }
      return result;
    }
  }

  public record AuthTokenProvider(
      boolean enabled, Optional<String> name, ImmutableMap<String, Boolean> users) {

    public AuthTokenProvider {
      Objects.requireNonNull(name, "enabled must not be null");
      users = users == null ? ImmutableMap.empty() : users;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("enabled", enabled);
      if (name.isPresent()) {
        result.put("name", name.get());
      }
      if (!users.isEmpty()) {
        result.put("users", users);
      }
      return result;
    }
  }

  public record AuthcDomain(
      Optional<Boolean> httpEnabled,
      Optional<Boolean> transportEnabled,
      int order,
      HttpAuthenticator httpAuthenticator,
      AuthenticationBackend authenticationBackend) {

    public AuthcDomain {
      Objects.requireNonNull(httpEnabled, "httpEnabled must not be null");
      Objects.requireNonNull(transportEnabled, "transportEnabled must not be null");
      Objects.requireNonNull(httpAuthenticator, "httpAuthenticator must not be null");
      Objects.requireNonNull(authenticationBackend, "authenticationBackend must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (httpEnabled.isPresent()) {
        result.put("http_enabled", httpEnabled.get());
      }
      if (transportEnabled.isPresent()) {
        result.put("transport_enabled", transportEnabled.get());
      }
      if (order >= 0) {
        result.put("order", order);
      }
      if (httpAuthenticator != null) {
        result.put("http_authenticator", httpAuthenticator.toBasicObject());
      }
      if (authenticationBackend != null) {
        result.put("authentication_backend", authenticationBackend.toBasicObject());
      }
      return result;
    }
  }

  public record AuthzDomain(
      Optional<Boolean> httpEnabled,
      Optional<Boolean> transportEnabled,
      AuthorizationBackend authorizationBackend) {

    public AuthzDomain {
      Objects.requireNonNull(httpEnabled, "httpEnabled must not be null");
      Objects.requireNonNull(transportEnabled, "httpEnabled must not be null");
      Objects.requireNonNull(authorizationBackend, "authorizationBackend must not be null");
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (httpEnabled.isPresent()) {
        result.put("http_enabled", httpEnabled.get());
      }
      if (transportEnabled.isPresent()) {
        result.put("transport_enabled", transportEnabled.get());
      }
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

    // stretch goals; kerberos, jwt, openid, clientcert, proxy, saml

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
        Optional<Boolean> enableSsl,
        Optional<Boolean> enableStartTls,
        Optional<Boolean> enableSslClientAuth,
        Optional<Boolean> verifyHostnames,
        ImmutableList<String> hosts,
        String bindDn,
        String password,
        String userbase,
        String usersearch,
        Optional<String> usernameAttribute)
        implements AuthenticationBackend {

      public Ldap {
        Objects.requireNonNull(enableSsl, "enableSsl must not be null");
        Objects.requireNonNull(enableStartTls, "enableStartTls must not be null");
        Objects.requireNonNull(enableSslClientAuth, "enableSslClientAuth must not be null");
        Objects.requireNonNull(verifyHostnames, "verifyHostnames must not be null");
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
        if (enableSsl.isPresent()) {
          config.put("enable_ssl", enableSsl.get());
        }
        if (enableStartTls.isPresent()) {
          config.put("enable_start_tls", enableStartTls.get());
        }
        if (enableSslClientAuth.isPresent()) {
          config.put("enable_ssl_client_auth", enableSslClientAuth.get());
        }
        if (verifyHostnames.isPresent()) {
          config.put("verify_hostnames", verifyHostnames.get());
        }
        if (!hosts.isEmpty()) {
          config.put("hosts", hosts);
        }
        if (!bindDn.isEmpty()) {
          config.put("bind_dn", bindDn);
        }
        if (!password.isEmpty()) {
          config.put("password", password);
        }
        if (!usernameAttribute.isPresent()) {
          config.put("username_attribute", usernameAttribute.get());
        }
        if (!usersearch.isEmpty()) {
          config.put("usersearch", usersearch);
        }
        if (usernameAttribute.isPresent()) {
          config.put("username_attribute", usernameAttribute.get());
        }
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
        Optional<Boolean> enableSsl,
        Optional<Boolean> enableStartTls,
        Optional<Boolean> enableSslClientAuth,
        Optional<Boolean> verifyHostnames,
        ImmutableList<String> hosts,
        String bindDn,
        String password,
        String userbase,
        String usersearch,
        Optional<String> usernameAttribute,
        String rolebase,
        String rolesearch,
        Optional<String> userroleattribute,
        Optional<String> userrolename,
        Optional<String> rolename,
        Optional<Boolean> resolveNestedRoles,
        ImmutableList<String> skipUsers)
        implements AuthorizationBackend {

      public Ldap {
        Objects.requireNonNull(enableSsl, "enableSsl must not be null");
        Objects.requireNonNull(enableStartTls, "enableStartTls must not be null");
        Objects.requireNonNull(enableSslClientAuth, "enableSslClientAuth must not be null");
        Objects.requireNonNull(verifyHostnames, "verifyHostnames must not be null");
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
        if (enableSsl.isPresent()) {
          config.put("enable_ssl", enableSsl.get());
        }
        if (enableStartTls.isPresent()) {
          config.put("enable_start_tls", enableStartTls.get());
        }
        if (enableSslClientAuth.isPresent()) {
          config.put("enable_ssl_client_auth", enableSslClientAuth.get());
        }
        if (verifyHostnames.isPresent()) {
          config.put("verify_hostnames", verifyHostnames.get());
        }
        if (!hosts.isEmpty()) {
          config.put("hosts", hosts);
        }
        if (!bindDn.isEmpty()) {
          config.put("bind_dn", bindDn);
        }
        if (!password.isEmpty()) {
          config.put("password", password);
        }
        if (!userbase.isEmpty()) {
          config.put("userbase", userbase);
        }
        if (!usersearch.isEmpty()) {
          config.put("usersearch", usersearch);
        }
        if (!usernameAttribute.isPresent()) {
          config.put("username_attribute", usernameAttribute.get());
        }
        if (!rolebase.isEmpty()) {
          config.put("rolebase", rolebase);
        }
        if (!rolename.isPresent()) {
          config.put("rolename", rolename.get());
        }
        if (!resolveNestedRoles.isPresent()) {
          config.put("resolve_nested_roles", resolveNestedRoles.get());
        }
        if (!skipUsers.isEmpty()) {
          config.put("skip_users", skipUsers);
        }
        result.put("config", config);
        return result;
      }
    }
  }
}
