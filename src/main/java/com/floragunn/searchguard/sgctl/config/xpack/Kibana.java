package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.*;

public record Kibana(SecurityConfig security) {

  public Kibana {
    Objects.requireNonNull(security, "security cannot be null");
  }

  public record SecurityConfig(
      boolean enabled,
      Optional<AuthCConfig> authC,
      Optional<String> loginAssistanceMessage,
      Optional<String> loginHelp,
      Optional<String> cookieName,
      Optional<String> encryptionKey,
      boolean secureCookies,
      Optional<SameSiteCookies> sameSiteCookies,
      CommonProviderData.Session session,
      Optional<DocNode> audit) {
    public SecurityConfig {
      Objects.requireNonNull(authC, "authC cannot be null");
      Objects.requireNonNull(loginAssistanceMessage, "loginAssistanceMessage cannot be null");
      Objects.requireNonNull(loginHelp, "loginHelp cannot be null");
      Objects.requireNonNull(cookieName, "cookieName cannot be null");
      Objects.requireNonNull(encryptionKey, "encryptionKey cannot be null");
      Objects.requireNonNull(sameSiteCookies, "sameSiteCookies cannot be null");
      Objects.requireNonNull(audit, "audit cannot be null");
    }

    public enum SameSiteCookies {
      Strict,
      Lax,
      None
    }

    private static SecurityConfig parse(DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
      var enabled = vDoc.get("enabled").withDefault(true).asBoolean();
      var authc = Optional.ofNullable(vDoc.get("authc").by(AuthCConfig::parse));
      var loginAssistanceMessage =
          Optional.ofNullable(vDoc.get("loginAssistanceMessage").asString());
      var loginHelp = Optional.ofNullable(vDoc.get("loginHelp").asString());
      var cookieName = Optional.ofNullable(vDoc.get("cookieName").asString());
      var encryptionKey = Optional.ofNullable(vDoc.get("encryptionKey").asString());
      var secureCookies = vDoc.get("secureCookies").withDefault(false).asBoolean();
      var sameSiteCookies =
          Optional.ofNullable(vDoc.get("sameSiteCookies").asEnum(SameSiteCookies.class));
      var session =
          vDoc.get("session")
              .withDefault(CommonProviderData.Session.EMPTY)
              .by(CommonProviderData.Session::parse);
      var audit = Optional.ofNullable(vDoc.get("audit").asDocNode());

      vDoc.throwExceptionForPresentErrors();

      return new SecurityConfig(
          enabled,
          authc,
          loginAssistanceMessage,
          loginHelp,
          cookieName,
          encryptionKey,
          secureCookies,
          sameSiteCookies,
          session,
          audit);
    }
  }

  public record AuthCConfig(
      Map<String, Provider> providers, boolean selectorEnabled, Optional<Http> http) {

    public AuthCConfig {
      Objects.requireNonNull(providers, "providers cannot be null");
      Objects.requireNonNull(http, "http cannot be null");
    }

    public record Http(boolean enabled, boolean autoSchemesEnabled, List<String> schemes) {

      public Http {
        Objects.requireNonNull(schemes, "schemes cannot be null");
      }

      private static Http parse(DocNode doc, Parser.Context context)
          throws ConfigValidationException {
        var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);

        var enabled = vDoc.get("enabled").withDefault(true).asBoolean();
        var autoSchemesEnabled = vDoc.get("autoSchemesEnabled").withDefault(true).asBoolean();
        var schemes = vDoc.get("schemes").withListDefault("apikey", "bearer").ofStrings();

        vDoc.throwExceptionForPresentErrors();

        return new Http(enabled, autoSchemesEnabled, schemes);
      }
    }

    private static AuthCConfig parse(DocNode doc, Parser.Context context)
        throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);

      Map<String, Provider> providers =
          vDoc.get("providers").by(ctx -> parseProviders(ctx, context));
      if (providers == null) {
        providers = Map.of();
      }
      var selectorEnabled =
          vDoc.get("selector.enabled").withDefault(providers.size() > 1).asBoolean();
      var http = Optional.ofNullable(vDoc.get("http").by(Http::parse));

      vDoc.throwExceptionForPresentErrors();

      return new AuthCConfig(providers, selectorEnabled, http);
    }

    private static ImmutableMap<String, Provider> parseProviders(
        DocNode doc, Parser.Context context) throws ConfigValidationException {
      var builder = new ImmutableMap.Builder<String, Provider>(doc.size());

      // structured as: xpack.security.authc.providers.<type>.<name>
      for (var providerType : doc.keySet()) {
        var typeNode = doc.getAsNode(providerType);
        for (var providerName : typeNode.keySet()) {
          var providerConfig = typeNode.getAsNode(providerName);
          var vDoc = new ValidatingDocNode(providerConfig, new ValidationErrors(), context);
          var provider = Provider.parse(providerType, providerName, vDoc);
          vDoc.throwExceptionForPresentErrors();
          builder.with(providerType + "." + providerName, provider);
        }
      }

      return builder.build();
    }
  }

  public record CommonProviderData(
      String name,
      int order,
      boolean enabled,
      Optional<String> description,
      Optional<String> hint,
      Optional<String> icon,
      List<String> origin,
      boolean showInSelector,
      Session session) {

    public CommonProviderData {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(description, "description cannot be null");
      Objects.requireNonNull(hint, "hint cannot be null");
      Objects.requireNonNull(icon, "icon cannot be null");
      Objects.requireNonNull(origin, "origin cannot be null");
      Objects.requireNonNull(session, "session cannot be null");
    }

    public record Session(
        Optional<String> idleTimeout,
        Optional<String> lifespan,
        Optional<String> cleanupInterval,
        Optional<Integer> maxConcurrentSessions) {

      public static final Session EMPTY =
          new Session(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

      public Session {
        Objects.requireNonNull(idleTimeout, "idleTimeout cannot be null");
        Objects.requireNonNull(lifespan, "lifespan cannot be null");
      }

      private static Session parse(DocNode doc, Parser.Context context)
          throws ConfigValidationException {
        var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);

        var idleTimeout = Optional.ofNullable(vDoc.get("idleTimeout").asString());
        var lifespan = Optional.ofNullable(vDoc.get("lifespan").asString());
        var cleanupInterval = Optional.ofNullable(vDoc.get("cleanupInterval").asString());
        var maxConcurrentSessions =
            Optional.ofNullable(vDoc.get("concurrentSessions.maxSessions").asInteger());

        vDoc.throwExceptionForPresentErrors();

        return new Session(idleTimeout, lifespan, cleanupInterval, maxConcurrentSessions);
      }
    }
  }

  public sealed interface Provider {

    String type();

    CommonProviderData common();

    record BasicProvider(String type, CommonProviderData common) implements Provider {
      public BasicProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
      }
    }

    record TokenProvider(String type, CommonProviderData common) implements Provider {
      public TokenProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
      }
    }

    record SamlProvider(
        String type,
        CommonProviderData common,
        String realm,
        Optional<Integer> maxRedirectURLSize,
        boolean useRelayStateDeepLink)
        implements Provider {
      public SamlProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
        Objects.requireNonNull(realm, "realm cannot be null");
      }

      private static SamlProvider parse(CommonProviderData common, ValidatingDocNode vDoc) {
        var realm = vDoc.get("realm").required().asString();
        var maxRedirectURLSize = Optional.ofNullable(vDoc.get("maxRedirectURLSize").asInteger());
        var useRelayStateDeepLink =
            vDoc.get("useRelayStateDeepLink").withDefault(false).asBoolean();
        return new SamlProvider("saml", common, realm, maxRedirectURLSize, useRelayStateDeepLink);
      }
    }

    record OidcProvider(String type, CommonProviderData common, String realm) implements Provider {
      public OidcProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
        Objects.requireNonNull(realm, "realm cannot be null");
      }

      private static OidcProvider parse(CommonProviderData common, ValidatingDocNode vDoc) {
        var realm = vDoc.get("realm").required().asString();
        return new OidcProvider("oidc", common, realm);
      }
    }

    record AnonymousProvider(
        String type, CommonProviderData common, String username, String password)
        implements Provider {
      public AnonymousProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
      }

      private static AnonymousProvider parse(CommonProviderData common, ValidatingDocNode vDoc) {
        var username = vDoc.get("credentials.username").required().asString();
        var password = vDoc.get("credentials.password").required().asString();
        return new AnonymousProvider("anonymous", common, username, password);
      }
    }

    // For stretch goals: kerberos, pki
    record OtherProvider(String type, CommonProviderData common, DocNode content)
        implements Provider {
      public OtherProvider {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(common, "common provider data cannot be null");
        Objects.requireNonNull(content, "content provider data cannot be null");
      }
    }

    private static Provider parse(String type, String name, ValidatingDocNode vDoc)
        throws ConfigValidationException {
      var order = vDoc.get("order").required().asInt();
      var enabled = vDoc.get("enabled").withDefault(true).asBoolean();
      var description = Optional.ofNullable(vDoc.get("description").asString());
      var hint = Optional.ofNullable(vDoc.get("hint").asString());
      var icon = Optional.ofNullable(vDoc.get("icon").asString());
      List<String> origin = vDoc.get("origin").withListDefault().ofStrings();
      var showInSelector =
          type.equals("basic")
              || type.equals("token")
              || vDoc.get("showInSelector").withDefault(true).asBoolean();
      var session =
          vDoc.get("session")
              .withDefault(CommonProviderData.Session.EMPTY)
              .by(CommonProviderData.Session::parse);

      var common =
          new CommonProviderData(
              name, order, enabled, description, hint, icon, origin, showInSelector, session);

      return switch (type) {
        case "basic" -> new BasicProvider(type, common);
        case "token" -> new TokenProvider(type, common);
        case "saml" -> SamlProvider.parse(common, vDoc);
        case "oidc" -> OidcProvider.parse(common, vDoc);
        case "anonymous" -> AnonymousProvider.parse(common, vDoc);
        default -> new OtherProvider(type, common, vDoc.getDocumentNode());
      };
    }
  }

  public static Kibana parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
    var errors = new ValidationErrors();
    var vDoc = new ValidatingDocNode(doc, errors, context);
    var xpackNode = vDoc.get("xpack").required().asDocNode();
    var security =
        new ValidatingDocNode(xpackNode, errors, context)
            .get("security")
            .by(Kibana.SecurityConfig::parse);

    errors.throwExceptionForPresentErrors();

    return new Kibana(security);
  }
}
