package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
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
    var security = config.security().get().orElseThrow();
    assertTrue(security.enabled().get());
    var session = security.session().get();
    assertEquals("30m", session.idleTimeout().orElse(null).get());
    assertEquals("8h", session.lifespan().orElse(null).get());
    assertTrue(security.sameSiteCookies().get().isPresent());
    assertEquals(
        Kibana.SecurityConfig.SameSiteCookies.Strict, security.sameSiteCookies().get().get());
    assertTrue(security.loginAssistanceMessage().get().isPresent());
    assertEquals("Testing!", security.loginAssistanceMessage().get().get());

    // Verify authc
    assertTrue(security.authC().get().isPresent());
    var authc = security.authC().get().get();
    assertTrue(authc.selectorEnabled().get());

    // Verify providerTypes
    var providerTypes = authc.providerTypes().get();
    assertNotNull(providerTypes);
    assertEquals(5, providerTypes.size());
    assertTrue(providerTypes.containsKey("anonymous"));
    assertTrue(providerTypes.containsKey("basic"));
    assertTrue(providerTypes.containsKey("saml"));
    assertTrue(providerTypes.containsKey("oidc"));
    assertTrue(providerTypes.containsKey("token"));

    // Verify anonymous provider
    var anonProv = providerTypes.get("anonymous").get().get("anonymous1").get();
    assertNotNull(anonProv);
    assertInstanceOf(Kibana.AuthCConfig.Provider.AnonymousProvider.class, anonProv);
    var anon = (Kibana.AuthCConfig.Provider.AnonymousProvider) anonProv;
    assertEquals("anonymous1", anon.common().name().get());
    assertEquals(0, anon.common().order().get());
    assertEquals("Anonymous access", anon.common().description().orElse(null).get());
    assertEquals("anonymous_service_account", anon.username().get());
    assertEquals("anonymous_service_account_password", anon.password().get());

    // Verify basic provider
    var basicProv = providerTypes.get("basic").get().get("basic1").get();
    assertNotNull(basicProv);
    assertInstanceOf(Kibana.AuthCConfig.Provider.BasicProvider.class, basicProv);
    var basic = (Kibana.AuthCConfig.Provider.BasicProvider) basicProv;
    assertEquals("basic1", basic.common().name().get());
    List<Traceable<String>> basicOrigins = basic.common().origin().get();
    assertNotNull(basicOrigins);
    assertEquals(2, basicOrigins.size());
    assertEquals("http://localhost:5601", basicOrigins.get(0).get());
    assertEquals("http://127.0.0.1:5601", basicOrigins.get(1).get());

    // Verify saml provider
    var samlProv = providerTypes.get("saml").get().get("saml1").get();
    assertNotNull(samlProv);
    assertInstanceOf(Kibana.AuthCConfig.Provider.SamlProvider.class, samlProv);
    var saml = (Kibana.AuthCConfig.Provider.SamlProvider) samlProv;
    assertEquals("saml1", saml.common().name().get());
    assertEquals(2, saml.common().order().get());
    assertEquals("saml1", saml.realm().get());
    // origin can be a single string turned into list
    ImmutableList<Traceable<String>> samlOrigins = saml.common().origin().get();
    assertEquals(1, samlOrigins.size());
    assertEquals("https://elastic.co", samlOrigins.get(0).get());

    // Verify oidc provider
    var oidcProv = providerTypes.get("oidc").get().get("oidc1").get();
    assertNotNull(oidcProv);
    assertInstanceOf(Kibana.AuthCConfig.Provider.OidcProvider.class, oidcProv);
    var oidc = (Kibana.AuthCConfig.Provider.OidcProvider) oidcProv;
    assertEquals("oidc1", oidc.common().name().get());
    assertEquals(3, oidc.common().order().get());
    assertEquals("oidc1", oidc.realm().get());
    assertEquals("OIDC Login", oidc.common().description().orElse(null).get());

    // Verify token providers
    var tokenProviders = providerTypes.get("token").get();
    var tokenProv = tokenProviders.get("token1").get();
    assertNotNull(tokenProv);
    assertInstanceOf(Kibana.AuthCConfig.Provider.TokenProvider.class, tokenProv);
    assertEquals("token1", tokenProv.common().name().get());
    assertEquals(4, tokenProv.common().order().get());

    var tokenProv2 = tokenProviders.get("token2").get();
    assertNotNull(tokenProv2);
    assertInstanceOf(Kibana.AuthCConfig.Provider.TokenProvider.class, tokenProv2);
    var token2 = (Kibana.AuthCConfig.Provider.TokenProvider) tokenProv2;
    assertEquals("token2", token2.common().name().get());
    assertEquals(5, token2.common().order().get());
  }
}
