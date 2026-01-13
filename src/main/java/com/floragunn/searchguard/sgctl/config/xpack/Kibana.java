package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.*;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Kibana(OptTraceable<SecurityConfig> security) {

  public Kibana {
    Objects.requireNonNull(security, "security cannot be null");
  }

  public record SecurityConfig(
      Traceable<Boolean> enabled,
      OptTraceable<AuthCConfig> authC,
      OptTraceable<String> loginAssistanceMessage,
      OptTraceable<String> loginHelp,
      OptTraceable<String> cookieName,
      OptTraceable<String> encryptionKey,
      Traceable<Boolean> secureCookies,
      OptTraceable<SameSiteCookies> sameSiteCookies,
      Traceable<Session> session,
      OptTraceable<DocNode> audit) {
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

    private static SecurityConfig parse(TraceableDocNode tDoc) {
      var enabled = tDoc.get("enabled").asBoolean(true);
      var authc = tDoc.get("authc").as(AuthCConfig::parse);
      var loginAssistanceMessage = tDoc.get("loginAssistanceMessage").asString();
      var loginHelp = tDoc.get("loginHelp").asString();
      var cookieName = tDoc.get("cookieName").asString();
      var encryptionKey = tDoc.get("encryptionKey").asString();
      var secureCookies = tDoc.get("secureCookies").asBoolean(false);
      var sameSiteCookies = tDoc.get("sameSiteCookies").asEnum(SameSiteCookies.class);
      var session = tDoc.get("session").as(Session::parse, Session.EMPTY);
      var audit = tDoc.get("audit").asDocNode();

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

  public record Session(
      OptTraceable<String> idleTimeout,
      OptTraceable<String> lifespan,
      OptTraceable<String> cleanupInterval,
      OptTraceable<Integer> maxConcurrentSessions) {

    public static final Session EMPTY =
        new Session(
            OptTraceable.empty(Source.NONE),
            OptTraceable.empty(Source.NONE),
            OptTraceable.empty(Source.NONE),
            OptTraceable.empty(Source.NONE));

    public Session {
      Objects.requireNonNull(idleTimeout, "idleTimeout cannot be null");
      Objects.requireNonNull(lifespan, "lifespan cannot be null");
    }

    private static Session parse(TraceableDocNode tDoc) {
      var idleTimeout = tDoc.get("idleTimeout").asString();
      var lifespan = tDoc.get("lifespan").asString();
      var cleanupInterval = tDoc.get("cleanupInterval").asString();
      var maxConcurrentSessions = tDoc.get("concurrentSessions.maxSessions").asInt();
      return new Session(idleTimeout, lifespan, cleanupInterval, maxConcurrentSessions);
    }
  }

  public record AuthCConfig(
      Traceable<ImmutableMap<String, Traceable<ImmutableMap<String, Traceable<Provider>>>>>
          providerTypes,
      Traceable<Boolean> selectorEnabled,
      OptTraceable<Http> http) {

    public AuthCConfig {
      Objects.requireNonNull(providerTypes, "providerTypes cannot be null");
      Objects.requireNonNull(http, "http cannot be null");
    }

    public record Http(
        Traceable<Boolean> enabled,
        Traceable<Boolean> autoSchemesEnabled,
        Traceable<ImmutableList<Traceable<String>>> schemes) {

      public Http {
        Objects.requireNonNull(schemes, "schemes cannot be null");
      }

      private static Http parse(TraceableDocNode tDoc) {
        var enabled = tDoc.get("enabled").asBoolean(true);
        var autoSchemesEnabled = tDoc.get("autoSchemesEnabled").asBoolean(true);
        var schemes = tDoc.get("schemes").asListOfStrings("apikey", "bearer");

        return new Http(enabled, autoSchemesEnabled, schemes);
      }
    }

    private static Traceable<ImmutableMap<String, Traceable<Provider>>> parseProviderType(
        TraceableAttribute.Required tAttr) {
      var type = Traceable.of(tAttr.getSource(), tAttr.getSource().pathPart());
      return tAttr.asMapOf(
          (TraceableDocNodeParser<Provider>)
              (providerTDoc) -> {
                var name =
                    Traceable.of(providerTDoc.getSource(), providerTDoc.getSource().pathPart());
                return Provider.parse(type, name, providerTDoc);
              });
    }

    private static AuthCConfig parse(TraceableDocNode tDoc) {
      var providerTypes =
          tDoc.get("providers").asMapOf(AuthCConfig::parseProviderType, ImmutableMap.empty());
      var selectorEnabled = tDoc.get("selector.enabled").asBoolean(true);
      var http = tDoc.get("http").as(Http::parse);

      return new AuthCConfig(providerTypes, selectorEnabled, http);
    }

    public record CommonProviderData(
        Traceable<String> type,
        Traceable<String> name,
        Traceable<Integer> order,
        Traceable<Boolean> enabled,
        OptTraceable<String> description,
        OptTraceable<String> hint,
        OptTraceable<String> icon,
        Traceable<ImmutableList<Traceable<String>>> origin,
        Traceable<Boolean> showInSelector,
        Traceable<Session> session) {

      public CommonProviderData {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(hint, "hint cannot be null");
        Objects.requireNonNull(icon, "icon cannot be null");
        Objects.requireNonNull(origin, "origin cannot be null");
        Objects.requireNonNull(session, "session cannot be null");
      }
    }

    public sealed interface Provider {

      default Traceable<String> type() {
        return common().type;
      }

      CommonProviderData common();

      record BasicProvider(CommonProviderData common) implements Provider {
        public BasicProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
        }
      }

      record TokenProvider(CommonProviderData common) implements Provider {
        public TokenProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
        }
      }

      record SamlProvider(
          CommonProviderData common,
          Traceable<String> realm,
          OptTraceable<Integer> maxRedirectURLSize,
          Traceable<Boolean> useRelayStateDeepLink)
          implements Provider {
        public SamlProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
          Objects.requireNonNull(realm, "realm cannot be null");
        }

        private static SamlProvider parse(CommonProviderData common, TraceableDocNode tDoc) {
          var realm = tDoc.get("realm").required().asString();
          var maxRedirectURLSize = tDoc.get("maxRedirectURLSize").asInt();
          var useRelayStateDeepLink = tDoc.get("useRelayStateDeepLink").asBoolean(false);
          return new SamlProvider(common, realm, maxRedirectURLSize, useRelayStateDeepLink);
        }
      }

      record OidcProvider(CommonProviderData common, Traceable<String> realm) implements Provider {
        public OidcProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
          Objects.requireNonNull(realm, "realm cannot be null");
        }

        private static OidcProvider parse(CommonProviderData common, TraceableDocNode tDoc) {
          var realm = tDoc.get("realm").required().asString();
          return new OidcProvider(common, realm);
        }
      }

      record AnonymousProvider(
          CommonProviderData common, Traceable<String> username, Traceable<String> password)
          implements Provider {
        public AnonymousProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
          Objects.requireNonNull(username, "username cannot be null");
          Objects.requireNonNull(password, "password cannot be null");
        }

        private static AnonymousProvider parse(CommonProviderData common, TraceableDocNode tDoc) {
          var username = tDoc.get("credentials.username").required().asString();
          var password = tDoc.get("credentials.password").required().asString();
          return new AnonymousProvider(common, username, password);
        }

        @Override
        public Traceable<String> type() {
          return common.name;
        }
      }

      // For stretch goals: kerberos, pki
      record OtherProvider(CommonProviderData common, Traceable<DocNode> content)
          implements Provider {
        public OtherProvider {
          Objects.requireNonNull(common, "common provider data cannot be null");
          Objects.requireNonNull(content, "content provider data cannot be null");
        }
      }

      private static Provider parse(
          Traceable<String> tType, Traceable<String> name, TraceableDocNode tDoc) {
        var order = tDoc.get("order").required().asInt();
        var enabled = tDoc.get("enabled").asBoolean(true);
        var description = tDoc.get("description").asString();
        var hint = tDoc.get("hint").asString();
        var icon = tDoc.get("icon").asString();
        var origin = tDoc.get("origin").asListOfStrings(ImmutableList.empty());
        Traceable<Boolean> showInSelector;
        var type = tType.get(); // Is ok, because created
        if (type.equals("basic") || type.equals("token")) {
          // Force true for basic and token provider, according to xpac docs
          showInSelector =
              Traceable.of(new Source.Attribute(tDoc.getSource(), "showInSelector"), true);
        } else {
          showInSelector = tDoc.get("showInSelector").asBoolean(true);
        }
        var session = tDoc.get("session").as(Session::parse, Session.EMPTY);
        var common =
            new CommonProviderData(
                tType,
                name,
                order,
                enabled,
                description,
                hint,
                icon,
                origin,
                showInSelector,
                session);

        return switch (type) {
          case "basic" -> new BasicProvider(common);
          case "token" -> new TokenProvider(common);
          case "saml" -> SamlProvider.parse(common, tDoc);
          case "oidc" -> OidcProvider.parse(common, tDoc);
          case "anonymous" -> AnonymousProvider.parse(common, tDoc);
          default -> new OtherProvider(common, tDoc.asAttribute().asDocNode());
        };
      }
    }
  }

  public static Kibana parse(TraceableDocNode tDoc) {
    return new Kibana(tDoc.get("xpack.security").as(Kibana.SecurityConfig::parse));
  }
}
