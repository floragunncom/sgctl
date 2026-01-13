package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.xpack.Kibana;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrator for X-Pack frontend authentication configuration to Search Guard frontend authc.
 * Maps Kibana authc providers and Elasticsearch realms to SgFrontendAuthC.
 */
@NullMarked
public class FrontendAuthMigrator implements SubMigrator {

    private static final Logger logger = LoggerFactory.getLogger(FrontendAuthMigrator.class);

    @Override
    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, MigrationReporter reporter) {
        return migrateInternal(context, reporter);
    }

    @Override
    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger unused) throws SgctlException {
        throw new MigrationNotImplementedException();
    }

    private List<NamedConfig<?>> migrateInternal(
            Migrator.IMigrationContext context, MigrationReporter reporter) {
        var kibanaOpt = context.getKibana();
        var elasticsearchOpt = context.getElasticsearch();


        if (kibanaOpt.isEmpty() && elasticsearchOpt.isEmpty()) {
            logger.debug("Skipping frontend auth migration: no Kibana or Elasticsearch configuration provided");
            reporter.problem("Skipping frontend auth migration: no Kibana or Elasticsearch configuration provided");
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
                    migrateKibanaProviders(authc, authDomainsBuilder, elasticsearchOpt);
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
            logger.debug("No frontend auth domains created");
            return List.of();
        }

        return List.of(new SgFrontendAuthC(authDomains));
    }

    private void migrateXPackRealms(
            XPackElasticsearchConfig.AuthcConfig authc,
            ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
            MigrationReporter reporter) {

        var realmsMap = authc.realms().get();

        var sortedRealms = realmsMap.values().stream()
                .flatMap(
                        realmType -> realmType.get().values().stream()
                                .map(realm -> java.util.Map.entry(realm.get().order(), realm)))
                .sorted(java.util.Comparator.comparingInt(entry -> entry.getKey().get()))
                .map(java.util.Map.Entry::getValue)
                .toList();

        boolean hasDefault = false;

        for (var realmTrace : sortedRealms) {
            var realm = realmTrace.get();

            if (!realm.enabled().get()) {
                logger.debug("Skipping disabled realm: {}", realm.name().get());
                continue;
            }

            boolean isDefault = !hasDefault;
            if (isDefault) {
                hasDefault = true;
            }

            if (realm instanceof XPackElasticsearchConfig.Realm.LdapRealm ldapRealm) {

                logger.debug("Skipping LDAP realm {}: no frontend auth equivalent", realm.name().get());
            } else if (realm instanceof XPackElasticsearchConfig.Realm.ActiveDirectoryRealm adRealm) {

                logger.debug("Skipping ActiveDirectory realm {}: no frontend auth equivalent", realm.name().get());
            } else if (realm instanceof XPackElasticsearchConfig.Realm.SAMLRealm samlRealm) {
                migrateSAMLRealmToFrontend(samlRealm, isDefault, authDomainsBuilder, reporter);
            } else if (realm instanceof XPackElasticsearchConfig.Realm.NativeRealm
                    || realm instanceof XPackElasticsearchConfig.Realm.FileRealm) {
                authDomainsBuilder.add(new SgFrontendAuthC.AuthDomain.Basic());
                logger.debug("Migrated {} realm to Basic auth domain", realm.type().get());
            } else {
                logger.warn("Skipping unsupported realm type: {}", realm.type().get());
                reporter.problem("Skipping unsupported realm type: " + realm.type().get());
            }
        }
    }

    private void migrateSAMLRealmToFrontend(
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

        if (idpEntityId.isEmpty() || spEntityId.isEmpty() || idpMetadataPath.isEmpty()) {
            logger.warn("Cannot migrate SAML realm {}: missing required fields (idp_entity_id, sp_entity_id, or idp.metadata.path)",
                    realm.name().get());
            reporter.problem("Cannot migrate SAML realm %s: missing required fields (idp_entity_id, sp_entity_id, or idp.metadata.path)"
                    .formatted(realm.name().get()));
            return;
        }

        var samlDomain = new SgFrontendAuthC.AuthDomain.Saml(
                label,
                id,
                isDefault,
                idpMetadataPath.get(),
                idpEntityId.get(),
                spEntityId.get());

        authDomainsBuilder.add(samlDomain);
        logger.debug("Migrated SAML realm {} to SAML auth domain", realm.name().get());
    }

    private void migrateKibanaProviders(
            Kibana.AuthCConfig authc,
            ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
            Optional<XPackElasticsearchConfig> elasticsearchOpt) {

        var providerTypes = authc.providerTypes().get();
        var selectorEnabled = authc.selectorEnabled().get();


        boolean hasMultipleProviders = providerTypes.values().stream()
                .mapToInt(providerMap -> providerMap.get().size())
                .sum() > 1;
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
                    logger.debug("Skipping disabled provider: {}.{}", providerType, providerName);
                    continue;
                }

                boolean isDefault = autoSelectFirst && isFirst;
                isFirst = false;

                switch (providerType) {
                    case "basic", "token" -> {
                        authDomainsBuilder.add(new SgFrontendAuthC.AuthDomain.Basic());
                        logger.debug("Migrated {} provider {} to Basic auth domain", providerType, providerName);
                    }
                    case "saml" -> {
                        if (provider instanceof Kibana.AuthCConfig.Provider.SamlProvider samlProvider) {
                            migrateSamlProvider(samlProvider, isDefault, authDomainsBuilder, elasticsearchOpt);
                        }
                    }
                    case "oidc" -> {
                        if (provider instanceof Kibana.AuthCConfig.Provider.OidcProvider oidcProvider) {
                            migrateOidcProvider(oidcProvider, isDefault, authDomainsBuilder, elasticsearchOpt);
                        }
                    }
                    default -> {
                        logger.warn("Skipping unsupported provider type: {}.{}", providerType, providerName);
                    }
                }
            }
        }
    }

    private void migrateSamlProvider(
            Kibana.AuthCConfig.Provider.SamlProvider samlProvider,
            boolean isDefault,
            ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
            Optional<XPackElasticsearchConfig> elasticsearchOpt) {

        var realmName = samlProvider.realm().get();
        var common = samlProvider.common();


        if (elasticsearchOpt.isEmpty()) {
            logger.warn("Cannot migrate SAML provider {}: Elasticsearch config not available for realm lookup",
                    common.name().get());
            return;
        }

        var elasticsearch = elasticsearchOpt.get();
        var realmMapOpt = elasticsearch.security().get().authc().get();

        if (realmMapOpt.isEmpty()) {
            logger.warn("Cannot migrate SAML provider {}: Elasticsearch authc config not available",
                    common.name().get());
            return;
        }

        var realms = realmMapOpt.get().realms().get();


        XPackElasticsearchConfig.Realm realm = null;
        for (var realmTypeEntry : realms.entrySet()) {
            var realmsByName = realmTypeEntry.getValue().get(); // ImmutableMap<String, Traceable<Realm>>

            for (var realmEntry : realmsByName.entrySet()) {
                var realmValue = realmEntry.getValue().get();
                if (realmEntry.getKey().equals(realmName) && realmValue instanceof XPackElasticsearchConfig.Realm.SAMLRealm) {
                    realm = realmValue;
                    break;
                }
            }
            if (realm != null) break;
        }

        if (realm == null || !(realm instanceof XPackElasticsearchConfig.Realm.SAMLRealm samlRealm)) {
            logger.warn("Cannot migrate SAML provider {}: realm {} not found or not a SAMLRealm",
                    common.name().get(), realmName);
            return;
        }


        var idpEntityId = samlRealm.idpEntityId().get();
        var spEntityId = samlRealm.spEntityId().get();
        var idpMetadataPath = samlRealm.idpMetadataPath().get();

        if (idpEntityId.isEmpty() || spEntityId.isEmpty() || idpMetadataPath.isEmpty()) {
            logger.warn("Cannot migrate SAML provider {}: missing required fields (idp_entity_id, sp_entity_id, or idp.metadata.path)",
                    common.name().get());
            return;
        }

        var label = common.description().get().or(() -> Optional.of("SAML Login"));
        var id = Optional.of(common.name().get());

        var samlDomain = new SgFrontendAuthC.AuthDomain.Saml(
                label,
                id,
                isDefault,
                idpMetadataPath.get(),
                idpEntityId.get(),
                spEntityId.get());

        authDomainsBuilder.add(samlDomain);
        logger.debug("Migrated SAML provider {} to SAML auth domain", common.name().get());
    }

    private void migrateOidcProvider(
            Kibana.AuthCConfig.Provider.OidcProvider oidcProvider,
            boolean isDefault,
            ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>> authDomainsBuilder,
            Optional<XPackElasticsearchConfig> elasticsearchOpt) {

        var realmName = oidcProvider.realm().get();
        var common = oidcProvider.common();


        if (elasticsearchOpt.isEmpty()) {
            logger.warn("Cannot migrate OIDC provider {}: Elasticsearch config not available for realm lookup",
                    common.name().get());
            return;
        }

        var elasticsearch = elasticsearchOpt.get();
        var realmMapOpt = elasticsearch.security().get().authc().get();

        if (realmMapOpt.isEmpty()) {
            logger.warn("Cannot migrate OIDC provider {}: Elasticsearch authc config not available",
                    common.name().get());
            return;
        }

        var realms = realmMapOpt.get().realms().get();


        XPackElasticsearchConfig.Realm realm = null;
        for (var realmTypeEntry : realms.entrySet()) {
            var realmsByName = realmTypeEntry.getValue().get(); // ImmutableMap<String, Traceable<Realm>>

            for (var realmEntry : realmsByName.entrySet()) {
                var realmValue = realmEntry.getValue().get();
                if (realmEntry.getKey().equals(realmName) && realmValue instanceof XPackElasticsearchConfig.Realm.GenericRealm) {
                    realm = realmValue;
                    break;
                }
            }
            if (realm != null) break;
        }

        if (realm == null || !(realm instanceof XPackElasticsearchConfig.Realm.GenericRealm genericRealm)) {
            logger.warn("Cannot migrate OIDC provider {}: realm {} not found or not a GenericRealm",
                    common.name().get(), realmName);
            return;
        }


        var rawConfig = genericRealm.rawConfig().get();
        var rpNode = rawConfig.getAsNode("rp");
        var opNode = rawConfig.getAsNode("op");

        var clientId = rpNode != null ? rpNode.getAsString("client_id") : null;
        var clientSecret = rpNode != null ? rpNode.getAsString("client_secret") : null;
        var openidConfigUrl = opNode != null ? opNode.getAsString("issuer") : null;

        if (clientId == null || clientSecret == null || openidConfigUrl == null) {
            logger.warn("Cannot migrate OIDC provider {}: missing required fields (client_id, client_secret, or issuer)",
                    common.name().get());
            return;
        }

        var label = common.description().get().or(() -> Optional.of("OIDC Login"));
        var id = Optional.of(common.name().get());

        var oidcDomain = new SgFrontendAuthC.AuthDomain.Oidc(
                label,
                id,
                isDefault,
                clientId,
                clientSecret,
                openidConfigUrl);

        authDomainsBuilder.add(oidcDomain);
        logger.debug("Migrated OIDC provider {} to OIDC auth domain", common.name().get());
    }
}
