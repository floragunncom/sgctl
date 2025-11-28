package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableMap;
import java.util.LinkedHashMap;
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
      Authenticator httpAuthenticator,
      Backend authenticationBackend,
      Backend authorizationBackend) {

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
      boolean httpEnabled, boolean transportEnabled, Backend authorizationBackend) {

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

  public record Authenticator(String type, boolean challenge, ImmutableMap<String, Object> config) {

    public Authenticator {
      Objects.requireNonNull(type, "type must not be null");
      config = config == null ? ImmutableMap.empty() : config;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("type", type);
      result.put("challenge", challenge);
      if (!config.isEmpty()) {
        result.put("config", config);
      }
      return result;
    }
  }

  public record Backend(String type, ImmutableMap<String, Object> config) {

    public Backend {
      Objects.requireNonNull(type, "type must not be null");
      config = config == null ? ImmutableMap.empty() : config;
    }

    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("type", type);
      if (!config.isEmpty()) {
        result.put("config", config);
      }
      return result;
    }
  }
}
