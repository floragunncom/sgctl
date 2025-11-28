package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgConfig.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SgConfigTest {

  private static final String SAMPLE_CONFIG_RESOURCE =
      "sgctl/config/searchguard/sample_sg_config.yml";
  private static final String SAMPLE_CONFIG_WITH_META_RESOURCE =
      "sgctl/config/searchguard/sample_sg_config_with_meta.yml";

  @Test
  void toBasicObjectMatchesSampleYaml() throws Exception {
    SgConfig config = loadConfig(SAMPLE_CONFIG_RESOURCE);
    Object expected = readYamlResource(SAMPLE_CONFIG_RESOURCE);

    assertEquals(expected, config.toBasicObject());
  }

  @Test
  void includesSgMetaWhenProvided() throws Exception {
    SgConfig config = loadConfig(SAMPLE_CONFIG_WITH_META_RESOURCE);
    Object expected = readYamlResource(SAMPLE_CONFIG_WITH_META_RESOURCE);

    assertEquals(expected, config.toBasicObject());
  }

  private static SgConfig loadConfig(String resourceName) throws Exception {
    Map<String, Object> yaml = readYamlResource(resourceName);
    Map<String, Object> searchGuard =
        Objects.requireNonNull(
            asMap(yaml.get("_searchguard")), "Missing _searchguard section in " + resourceName);

    return new SgConfig(
        immutableOrEmpty(asMap(yaml.get("_sg_meta"))),
        new SearchGuard(
            (Boolean) searchGuard.get("xpack_migration_mode"),
            (String) searchGuard.get("license"),
            parseDynamic(asMap(searchGuard.get("dynamic")))));
  }

  private static Dynamic parseDynamic(Map<String, Object> dynamic) {
    Map<String, Object> dynamicMap = dynamic == null ? Map.of() : dynamic;

    return new Dynamic(
        (String) dynamicMap.get("filtered_alias_mode"),
        (Boolean) dynamicMap.get("multi_rolespan_enabled"),
        (String) dynamicMap.get("hosts_resolver_mode"),
        (Boolean) dynamicMap.get("do_not_fail_on_forbidden"),
        (Boolean) dynamicMap.get("do_not_fail_on_forbidden_empty"),
        parseKibana(asMap(dynamicMap.get("kibana"))),
        parseHttp(asMap(dynamicMap.get("http"))),
        parseAuthTokenProvider(asMap(dynamicMap.get("auth_token_provider"))),
        parseAuthc(asMap(dynamicMap.get("authc"))),
        parseAuthz(asMap(dynamicMap.get("authz"))));
  }

  private static Kibana parseKibana(Map<String, Object> kibana) {
    if (kibana == null) {
      return null;
    }

    return new Kibana(
        (Boolean) kibana.get("multitenancy_enabled"),
        (String) kibana.get("server_username"),
        (String) kibana.get("index"));
  }

  private static Http parseHttp(Map<String, Object> http) {
    if (http == null) {
      return null;
    }

    return new Http((Boolean) http.get("anonymous_auth_enabled"), parseXff(asMap(http.get("xff"))));
  }

  private static Xff parseXff(Map<String, Object> xff) {
    if (xff == null) {
      return null;
    }

    return new Xff(
        (Boolean) xff.get("enabled"),
        (String) xff.get("remoteIpHeader"),
        (String) xff.get("internalProxies"));
  }

  private static AuthTokenProvider parseAuthTokenProvider(Map<String, Object> authTokenProvider) {
    if (authTokenProvider == null) {
      return null;
    }

    return new AuthTokenProvider(
        (Boolean) authTokenProvider.get("enabled"),
        (String) authTokenProvider.get("name"),
        immutableBooleanMap(asMap(authTokenProvider.get("users"))));
  }

  private static ImmutableMap<String, AuthcDomain> parseAuthc(Map<String, Object> authc) {
    if (authc == null || authc.isEmpty()) {
      return ImmutableMap.empty();
    }

    Map<String, AuthcDomain> result = new LinkedHashMap<>();
    authc.forEach((name, value) -> result.put(name, parseAuthcDomain(asMap(value))));
    return ImmutableMap.of(result);
  }

  private static AuthcDomain parseAuthcDomain(Map<String, Object> authcDomain) {
    Map<String, Object> authc = authcDomain == null ? Map.of() : authcDomain;

    return new AuthcDomain(
        booleanValue(authc.get("http_enabled")),
        booleanValue(authc.get("transport_enabled")),
        intValue(authc.get("order")),
        parseAuthenticator(asMap(authc.get("http_authenticator"))),
        parseBackend(asMap(authc.get("authentication_backend"))),
        authc.containsKey("authorization_backend")
            ? parseBackend(asMap(authc.get("authorization_backend")))
            : null);
  }

  private static HttpAuthenticator parseAuthenticator(Map<String, Object> authenticator) {
    if (authenticator == null) {
      throw new IllegalArgumentException("Authenticator config missing in sample config");
    }

    return new HttpAuthenticator(
        HttpAuthenticator.Type.valueOf(authenticator.get("type").toString().toUpperCase(Locale.ROOT)),
        booleanValue(authenticator.get("challenge")),
        immutableOrEmpty(asMap(authenticator.get("config"))));
  }

  private static Backend parseBackend(Map<String, Object> backend) {
    if (backend == null) {
      throw new IllegalArgumentException("Backend config missing in sample config");
    }

    return new Backend(
        Backend.Type.valueOf(backend.get("type").toString().toUpperCase(Locale.ROOT)),
        immutableOrEmpty(asMap(backend.get("config"))));
  }

  private static ImmutableMap<String, AuthzDomain> parseAuthz(Map<String, Object> authz) {
    if (authz == null || authz.isEmpty()) {
      return ImmutableMap.empty();
    }

    Map<String, AuthzDomain> result = new LinkedHashMap<>();
    authz.forEach((name, value) -> result.put(name, parseAuthzDomain(asMap(value))));
    return ImmutableMap.of(result);
  }

  private static AuthzDomain parseAuthzDomain(Map<String, Object> authzDomain) {
    Map<String, Object> authz = authzDomain == null ? Map.of() : authzDomain;

    return new AuthzDomain(
        booleanValue(authz.get("http_enabled")),
        booleanValue(authz.get("transport_enabled")),
        parseBackend(asMap(authz.get("authorization_backend"))));
  }

  private static Map<String, Object> readYamlResource(String resourceName) throws Exception {
    try (InputStream input =
        SgConfigTest.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing resource " + resourceName);
      }

      String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      return DocReader.yaml().readObject(yaml);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    return (Map<String, Object>) value;
  }

  private static ImmutableMap<String, Object> immutableOrEmpty(Map<String, Object> map) {
    return map == null ? ImmutableMap.empty() : ImmutableMap.of(map);
  }

  private static ImmutableMap<String, Boolean> immutableBooleanMap(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return ImmutableMap.empty();
    }

    Map<String, Boolean> result = new LinkedHashMap<>();
    map.forEach((key, value) -> result.put(key, booleanValue(value)));
    return ImmutableMap.of(result);
  }

  private static boolean booleanValue(Object value) {
    return value != null && (Boolean) value;
  }

  private static int intValue(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }
}
