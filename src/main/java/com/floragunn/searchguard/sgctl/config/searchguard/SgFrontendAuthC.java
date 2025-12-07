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
     * @param IDPEntityId Entity id for the identity provider.
     * @param SPEntityId Entity id for the service provider.
     */
    public record SAML(
        Optional<String> label,
        Optional<String> id,
        Boolean isDefault,
        String metadataURL,
        String IDPEntityId,
        String SPEntityId)
        implements AuthDomain<SAML> {

      public SAML {
        Objects.requireNonNull(isDefault, "isDefault must not be null");
        Objects.requireNonNull(metadataURL, "metadataURL must not be null");
        Objects.requireNonNull(IDPEntityId, "IDPEntityId must not be null");
        Objects.requireNonNull(SPEntityId, "SPEntityId must not be null");
      }

      @Override
      public Object toBasicObject() {
        var builder = new OrderedImmutableMap.Builder<String, Object>();
        builder.put("type", "saml");
        builder.put("saml.idp.metadata_url", metadataURL);
        builder.put("saml.idp.entity_id", IDPEntityId);
        builder.put("saml.sp.entity_id", SPEntityId);
        id.ifPresent(id -> builder.put("id", id));
        label.ifPresent(gs -> builder.put("label", label));
        if (isDefault) builder.put("auto_select", true);
        return builder.build();
      }
    }
  }
}
