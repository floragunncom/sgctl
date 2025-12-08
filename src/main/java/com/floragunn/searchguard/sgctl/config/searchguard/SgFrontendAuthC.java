package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.Document;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.OrderedImmutableMap;
import java.util.Objects;
import java.util.Optional;

public record SgFrontendAuthC(ImmutableList<AuthDomain<?>> authDomains)
    implements NamedConfig<SgFrontendAuthC> {

  public SgFrontendAuthC {
    Objects.requireNonNull(authDomains, "authDomains must not be null");
  }

  @Override
  public String getFileName() {
    return "sg_frontend_authc.yml";
  }

  @Override
  public Object toBasicObject() {
    var builder = new OrderedImmutableMap.Builder<String, Object>();
    var authDomainsBuilder = new OrderedImmutableMap.Builder<String, Object>();
    authDomainsBuilder.put("auth_domains", authDomains);
    builder.put("default", authDomainsBuilder.build());
    return builder.build();
  }

  public interface AuthDomain<T> extends Document<T> {
    /** The default authentication method for a Kibana Instance. */
    public record Basic() implements AuthDomain<Basic> {
      @Override
      public Object toBasicObject() {
        return OrderedImmutableMap.of("type", "basic");
      }
    }

    /**
     * SAML authentication to set up SSO.
     *
     * @param label A label for the authentication method. Defaults to "SAML Login".
     * @param id An optional id. Only needed when multiple SAML configs are used.
     * @param isDefault Is this the default authentication method for Kibana?
     * @param metadataURL Metadata URL for the identity provider.
     * @param idpEntityId Entity id for the identity provider.
     * @param spEntityId Entity id for the service provider.
     */
    public record Saml(
        Optional<String> label,
        Optional<String> id,
        Boolean isDefault,
        String metadataURL,
        String idpEntityId,
        String spEntityId)
        implements AuthDomain<Saml> {

      public Saml {
        Objects.requireNonNull(isDefault, "isDefault must not be null");
        Objects.requireNonNull(metadataURL, "metadataURL must not be null");
        Objects.requireNonNull(idpEntityId, "idpEntityId must not be null");
        Objects.requireNonNull(spEntityId, "spEntityId must not be null");
      }

      @Override
      public Object toBasicObject() {
        var builder = new OrderedImmutableMap.Builder<String, Object>();
        builder.put("type", "saml");
        builder.put("saml.idp.metadata_url", metadataURL);
        builder.put("saml.idp.entity_id", idpEntityId);
        builder.put("saml.sp.entity_id", spEntityId);
        builder.put("label", label.orElse("SAML Login"));
        id.ifPresent(id -> builder.put("id", id));
        if (isDefault) builder.put("auto_select", true);
        return builder.build();
      }
    }

    /**
     * OIDC authentication for Kibana
     *
     * @param label A label for the authentication method. Defaults to "OIDC Login".
     * @param id An optional id. Only needed when multiple OIDC configs are used.
     * @param isDefault Is this the default authentication method for Kibana?
     * @param clientId The identity providers client id.
     * @param clientSecret The identity providers client secret.
     * @param openidConfigurationUrl The url pointing to the OpenID Connect configuration.
     */
    public record Oidc(
        Optional<String> label,
        Optional<String> id,
        Boolean isDefault,
        String clientId,
        String clientSecret,
        String openidConfigurationUrl)
        implements AuthDomain<Oidc> {

      public Oidc {
        Objects.requireNonNull(isDefault, "isDefault must not be null");
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        Objects.requireNonNull(openidConfigurationUrl, "openidConfigurationUrl must not be null");
      }

      @Override
      public Object toBasicObject() {
        var builder = new OrderedImmutableMap.Builder<String, Object>();
        builder.put("type", "oidc");
        builder.put("oidc.client_id", clientId);
        builder.put("oidc.client_secret", clientSecret);
        builder.put("oidc.idp.openid_configuration_url", openidConfigurationUrl);
        builder.put("label", label.orElse("OIDC Login"));
        id.ifPresent(id -> builder.put("id", id));
        if (isDefault) builder.put("auto_select", true);
        return builder.build();
      }
    }
  }
}
