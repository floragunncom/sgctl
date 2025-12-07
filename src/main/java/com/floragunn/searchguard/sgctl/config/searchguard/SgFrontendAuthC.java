package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.Document;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.OrderedImmutableMap;
import java.util.Objects;

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
    /** The default authentication method for a Kibana Instance */
    public record Basic() implements AuthDomain<Basic> {
      @Override
      public Object toBasicObject() {
        return OrderedImmutableMap.of("type", "basic");
      }
    }
  }
}
