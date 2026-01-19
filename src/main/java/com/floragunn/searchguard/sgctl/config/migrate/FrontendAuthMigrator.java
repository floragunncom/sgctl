package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.Kibana;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Migrator for X-Pack frontend authentication configuration to Search Guard frontend authc. Maps
 * Kibana authc providers and Elasticsearch realms to SgFrontendAuthC.
 */
@NullMarked
public class FrontendAuthMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    return migrateInternal(context, reporter);
  }

  private List<NamedConfig<?>> migrateInternal(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    var kibanaOpt = context.getKibana();
    var elasticsearchOpt = context.getElasticsearch();

    if (kibanaOpt.isEmpty() && elasticsearchOpt.isEmpty()) {
      reporter.problem(
          "Skipping frontend auth migration: no Kibana or Elasticsearch configuration provided");
      return List.of();
    }

    var authDomainsBuilder = new ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>>();

    if (kibanaOpt.isPresent()) {
      var kibana = kibanaOpt.get();
      var securityOpt = kibana.security().get();

      if (securityOpt.isPresent()) {
        var security = securityOpt.get();
        var authcOpt = security.authC().get();

        if (authcOpt.isPresent()) {
          var authc = authcOpt.get();
          migrateKibanaProviders(authc, authDomainsBuilder, elasticsearchOpt, reporter);
        }
      }
    }

    var initialDomains = authDomainsBuilder.build();

    if (initialDomains.isEmpty() && elasticsearchOpt.isPresent()) {
      var elasticsearch = elasticsearchOpt.get();
      var securityTrace = elasticsearch.security();
      var securityOpt = securityTrace.get();
      if (securityOpt != null && securityOpt.authc().get().isPresent()) {
        migrateXPackRealms(securityOpt.authc().get().get(), authDomainsBuilder, reporter);
      }
    }

    var authDomains = authDomainsBuilder.build();

    if (authDomains.isEmpty()) {
      return List.of();
    }

    return List.of(new SgFrontendAuthC(authDomains));
  }

  private void migrateXPackRealms(
      XPackElasticsearchConfig.AuthcConfig authc,
      ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
      MigrationReporter reporter) {

    var realmsMap = authc.realms().get();

    var sortedRealms =
        realmsMap.values().stream()
            .flatMap(
                realmType ->
                    realmType.get().values().stream()
                        .map(realm -> Map.entry(realm.get().order(), realm)))
            .sorted(Comparator.comparingInt(entry -> entry.getKey().get()))
            .map(Map.Entry::getValue)
            .toList();

    boolean hasDefault = false;

    for (var realmTrace : sortedRealms) {
      var realm = realmTrace.get();

      if (!realm.enabled().get()) {
        continue;
      }

      boolean isDefault = !hasDefault;

      if (realm instanceof XPackElasticsearchConfig.Realm.LdapRealm) {
        reporter.problem(realmTrace, "LDAP realm has no frontend auth equivalent - skipping");
      } else if (realm instanceof XPackElasticsearchConfig.Realm.ActiveDirectoryRealm) {
        reporter.problem(
            realmTrace, "ActiveDirectory realm has no frontend auth equivalent - skipping");
      } else if (realm instanceof XPackElasticsearchConfig.Realm.SAMLRealm samlRealm) {
        migrateSAMLRealmToFrontend(realmTrace, samlRealm, isDefault, authDomainsBuilder, reporter);
        if (isDefault) hasDefault = true;
      } else if (realm instanceof XPackElasticsearchConfig.Realm.NativeRealm
          || realm instanceof XPackElasticsearchConfig.Realm.FileRealm) {
        authDomainsBuilder.add(new SgFrontendAuthC.AuthDomain.Basic());
        if (isDefault) hasDefault = true;
      } else {
        reporter.problem(
            realmTrace, "Unsupported realm type: " + realm.type().get() + " - skipping");
      }
    }
  }

  private void migrateSAMLRealmToFrontend(
      Traceable<? extends XPackElasticsearchConfig.Realm> realmTrace,
      XPackElasticsearchConfig.Realm.SAMLRealm realm,
      boolean isDefault,
      ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
      MigrationReporter reporter) {

    var label = Optional.of(realm.name().get());
    var id = Optional.of(realm.name().get());

    // Extract SAML configuration
    var idpEntityId = realm.idpEntityId().get();
    var spEntityId = realm.spEntityId().get();
    var idpMetadataPath = realm.idpMetadataPath().get();
    var subjectKey = realm.attributesPrincipal().get();
    var rolesKey = realm.attributesGroups().get();
    var kibanaUrl = realm.spAcs().get().map(FrontendAuthMigrator::extractKibanaUrlFromAcs);

    if (idpEntityId.isEmpty() || spEntityId.isEmpty() || idpMetadataPath.isEmpty()) {
      reporter.problem(
          realmTrace,
          "Missing required SAML fields (idp_entity_id, sp_entity_id, or idp.metadata.path)");
      return;
    }

    var samlDomain =
        new SgFrontendAuthC.AuthDomain.Saml(
            label,
            id,
            isDefault,
            idpMetadataPath.get(),
            idpEntityId.get(),
            spEntityId.get(),
            subjectKey,
            rolesKey,
            kibanaUrl);

    authDomainsBuilder.add(samlDomain);
  }

  private void migrateKibanaProviders(
      Kibana.AuthCConfig authc,
      ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
      Optional<XPackElasticsearchConfig> elasticsearchOpt,
      MigrationReporter reporter) {

    var providerTypes = authc.providerTypes().get();
    var selectorEnabled = authc.selectorEnabled().get();

    boolean hasMultipleProviders =
        providerTypes.values().stream().mapToInt(providerMap -> providerMap.get().size()).sum() > 1;
    boolean autoSelectFirst = !selectorEnabled && !hasMultipleProviders;

    boolean isFirst = true;

    for (var providerTypeEntry : providerTypes.entrySet()) {
      var providerType = providerTypeEntry.getKey();
      var providers = providerTypeEntry.getValue().get();

      for (var providerEntry : providers.entrySet()) {
        var providerName = providerEntry.getKey();
        var provider = providerEntry.getValue().get();

        var common = provider.common();
        if (!common.enabled().get()) {
          continue;
        }

        boolean isDefault = autoSelectFirst && isFirst;
        isFirst = false;

        switch (providerType) {
          case "basic", "token" -> {
            authDomainsBuilder.add(new SgFrontendAuthC.AuthDomain.Basic());
          }
          case "saml" -> {
            if (provider instanceof Kibana.AuthCConfig.Provider.SamlProvider samlProvider) {
              migrateSamlProvider(
                  samlProvider, isDefault, authDomainsBuilder, elasticsearchOpt, reporter);
            }
          }
          case "oidc" -> {
            if (provider instanceof Kibana.AuthCConfig.Provider.OidcProvider oidcProvider) {
              migrateOidcProvider(
                  oidcProvider, isDefault, authDomainsBuilder, elasticsearchOpt, reporter);
            }
          }
          default -> {
            reporter.problem(
                "Skipping unsupported provider type: %s.%s".formatted(providerType, providerName));
          }
        }
      }
    }
  }

  private void migrateSamlProvider(
      Kibana.AuthCConfig.Provider.SamlProvider samlProvider,
      boolean isDefault,
      ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
      Optional<XPackElasticsearchConfig> elasticsearchOpt,
      MigrationReporter reporter) {

    var realmName = samlProvider.realm().get();
    var common = samlProvider.common();

    if (elasticsearchOpt.isEmpty()) {
      reporter.problem(
          "Cannot migrate SAML provider %s: Elasticsearch config not available for realm lookup"
              .formatted(common.name().get()));
      return;
    }

    var elasticsearch = elasticsearchOpt.get();
    var realmMapOpt = elasticsearch.security().get().authc().get();

    if (realmMapOpt.isEmpty()) {
      reporter.problem(
          "Cannot migrate SAML provider %s: Elasticsearch authc config not available"
              .formatted(common.name().get()));
      return;
    }

    var realms = realmMapOpt.get().realms().get();

    XPackElasticsearchConfig.Realm realm = null;
    for (var realmTypeEntry : realms.entrySet()) {
      var realmsByName = realmTypeEntry.getValue().get(); // ImmutableMap<String, Traceable<Realm>>

      for (var realmEntry : realmsByName.entrySet()) {
        var realmValue = realmEntry.getValue().get();
        if (realmEntry.getKey().equals(realmName)
            && realmValue instanceof XPackElasticsearchConfig.Realm.SAMLRealm) {
          realm = realmValue;
          break;
        }
      }
      if (realm != null) break;
    }

    if (realm == null || !(realm instanceof XPackElasticsearchConfig.Realm.SAMLRealm samlRealm)) {
      reporter.problem(
          "Cannot migrate SAML provider %s: realm %s not found or not a SAMLRealm"
              .formatted(common.name().get(), realmName));
      return;
    }

    var idpEntityId = samlRealm.idpEntityId().get();
    var spEntityId = samlRealm.spEntityId().get();
    var idpMetadataPath = samlRealm.idpMetadataPath().get();
    var subjectKey = samlRealm.attributesPrincipal().get();
    var rolesKey = samlRealm.attributesGroups().get();
    var kibanaUrl = samlRealm.spAcs().get().map(FrontendAuthMigrator::extractKibanaUrlFromAcs);

    if (idpEntityId.isEmpty() || spEntityId.isEmpty() || idpMetadataPath.isEmpty()) {
      reporter.problem(
          "Cannot migrate SAML provider %s: missing required fields (idp_entity_id, sp_entity_id, or idp.metadata.path)"
              .formatted(common.name().get()));
      return;
    }

    var label = common.description().get().or(() -> Optional.of("SAML Login"));
    var id = Optional.of(common.name().get());

    var samlDomain =
        new SgFrontendAuthC.AuthDomain.Saml(
            label,
            id,
            isDefault,
            idpMetadataPath.get(),
            idpEntityId.get(),
            spEntityId.get(),
            subjectKey,
            rolesKey,
            kibanaUrl);

    authDomainsBuilder.add(samlDomain);
  }

  private void migrateOidcProvider(
      Kibana.AuthCConfig.Provider.OidcProvider oidcProvider,
      boolean isDefault,
      ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
      Optional<XPackElasticsearchConfig> elasticsearchOpt,
      MigrationReporter reporter) {

    var realmName = oidcProvider.realm().get();
    var common = oidcProvider.common();

    if (elasticsearchOpt.isEmpty()) {
      reporter.problem(
          "Cannot migrate OIDC provider %s: Elasticsearch config not available for realm lookup"
              .formatted(common.name().get()));
      return;
    }

    var elasticsearch = elasticsearchOpt.get();
    var realmMapOpt = elasticsearch.security().get().authc().get();

    if (realmMapOpt.isEmpty()) {
      reporter.problem(
          "Cannot migrate OIDC provider %s: Elasticsearch authc config not available"
              .formatted(common.name().get()));
      return;
    }

    var realms = realmMapOpt.get().realms().get();

    XPackElasticsearchConfig.Realm realm = null;
    for (var realmTypeEntry : realms.entrySet()) {
      var realmsByName = realmTypeEntry.getValue().get(); // ImmutableMap<String, Traceable<Realm>>

      for (var realmEntry : realmsByName.entrySet()) {
        var realmValue = realmEntry.getValue().get();
        if (realmEntry.getKey().equals(realmName)
            && realmValue instanceof XPackElasticsearchConfig.Realm.GenericRealm) {
          realm = realmValue;
          break;
        }
      }
      if (realm != null) break;
    }

    if (realm == null
        || !(realm instanceof XPackElasticsearchConfig.Realm.GenericRealm genericRealm)) {
      reporter.problem(
          "Cannot migrate OIDC provider %s: realm %s not found or not a GenericRealm"
              .formatted(common.name().get(), realmName));
      return;
    }

    var rawConfig = genericRealm.rawConfig().get();
    var rpNode = rawConfig.getAsNode("rp");
    var opNode = rawConfig.getAsNode("op");

    var clientId = rpNode != null ? rpNode.getAsString("client_id") : null;
    var clientSecret = rpNode != null ? rpNode.getAsString("client_secret") : null;
    var openidConfigUrl = opNode != null ? opNode.getAsString("issuer") : null;

    if (clientId == null || clientSecret == null || openidConfigUrl == null) {
      reporter.problem(
          "Cannot migrate OIDC provider %s: missing required fields (client_id, client_secret, or issuer)"
              .formatted(common.name().get()));
      return;
    }

    var label = common.description().get().or(() -> Optional.of("OIDC Login"));
    var id = Optional.of(common.name().get());

    var oidcDomain =
        new SgFrontendAuthC.AuthDomain.Oidc(
            label, id, isDefault, clientId, clientSecret, openidConfigUrl);

    authDomainsBuilder.add(oidcDomain);
  }

  /**
   * Extracts base Kibana URL from ACS endpoint (e.g. ".../api/security/saml/callback" → base URL).
   */
  private static String extractKibanaUrlFromAcs(String acsUrl) {
    try {
      var uri = java.net.URI.create(acsUrl);
      var port = uri.getPort();
      var portPart = (port > 0 && port != 80 && port != 443) ? ":" + port : "";
      return uri.getScheme() + "://" + uri.getHost() + portPart + "/";
    } catch (Exception e) {
      // If parsing fails, return the original URL as-is
      return acsUrl;
    }
  }
}
