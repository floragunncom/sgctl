package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class KibanaTest {

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return DocNode.wrap(DocReader.yaml().read(in));
    }
  }

  @Test
  public void testParseBasic() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/kibana/basic.yml");
    var config = Kibana.parse(node, Parser.Context.get());

    assertNotNull(config);

    // Verify security
    var security = config.security();
    assertTrue(security.enabled());
    var session = security.session();
    assertEquals("30m", session.idleTimeout().orElse(null));
    assertEquals("8h", session.lifespan().orElse(null));
    assertTrue(security.sameSiteCookies().isPresent());
    assertEquals(Kibana.SecurityConfig.SameSiteCookies.Strict, security.sameSiteCookies().get());
    assertTrue(security.loginAssistanceMessage().isPresent());
    assertEquals("Testing!", security.loginAssistanceMessage().get());

    // Verify authc
    assertTrue(security.authC().isPresent());
    var authc = security.authC().get();
    assertTrue(authc.selectorEnabled());

    // Verify providers
    Map<String, Kibana.Provider> providers = authc.providers();
    assertNotNull(providers);
    assertEquals(5, providers.size());
    assertTrue(providers.containsKey("anonymous.anonymous1"));
    assertTrue(providers.containsKey("basic.basic1"));
    assertTrue(providers.containsKey("saml.saml1"));
    assertTrue(providers.containsKey("oidc.oidc1"));
    assertTrue(providers.containsKey("token.token1"));

    // Verify anonymous provider
    var anonProv = providers.get("anonymous.anonymous1");
    assertNotNull(anonProv);
    assertInstanceOf(Kibana.Provider.AnonymousProvider.class, anonProv);
    var anon = (Kibana.Provider.AnonymousProvider) anonProv;
    assertEquals("anonymous1", anon.common().name());
    assertEquals(0, anon.common().order());
    assertEquals("Anonymous access", anon.common().description().orElse(null));
    assertEquals("anonymous_service_account", anon.username());
    assertEquals("anonymous_service_account_password", anon.password());

    // Verify basic provider
    var basicProv = providers.get("basic.basic1");
    assertNotNull(basicProv);
    assertInstanceOf(Kibana.Provider.BasicProvider.class, basicProv);
    var basic = (Kibana.Provider.BasicProvider) basicProv;
    assertEquals("basic1", basic.common().name());
    List<String> basicOrigins = basic.common().origin();
    assertNotNull(basicOrigins);
    assertEquals(2, basicOrigins.size());
    assertTrue(basicOrigins.contains("http://localhost:5601"));
    assertTrue(basicOrigins.contains("http://127.0.0.1:5601"));

    // Verify saml provider
    var samlProv = providers.get("saml.saml1");
    assertNotNull(samlProv);
    assertInstanceOf(Kibana.Provider.SamlProvider.class, samlProv);
    var saml = (Kibana.Provider.SamlProvider) samlProv;
    assertEquals("saml1", saml.common().name());
    assertEquals(2, saml.common().order());
    assertEquals("saml1", saml.realm());
    // origin can be a single string turned into list
    List<String> samlOrigins = saml.common().origin();
    assertEquals(1, samlOrigins.size());
    assertEquals("https://elastic.co", samlOrigins.get(0));

    // Verify oidc provider
    var oidcProv = providers.get("oidc.oidc1");
    assertNotNull(oidcProv);
    assertInstanceOf(Kibana.Provider.OidcProvider.class, oidcProv);
    var oidc = (Kibana.Provider.OidcProvider) oidcProv;
    assertEquals("oidc1", oidc.common().name());
    assertEquals(3, oidc.common().order());
    assertEquals("oidc1", oidc.realm());
    assertEquals("OIDC Login", oidc.common().description().orElse(null));

    // Verify token provider
    var tokenProv = providers.get("token.token1");
    assertNotNull(tokenProv);
    assertInstanceOf(Kibana.Provider.TokenProvider.class, tokenProv);
    var token = (Kibana.Provider.TokenProvider) tokenProv;
    assertEquals("token1", token.common().name());
    assertEquals(4, token.common().order());
  }
}
