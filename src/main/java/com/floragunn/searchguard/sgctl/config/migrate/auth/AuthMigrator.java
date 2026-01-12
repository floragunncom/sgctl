package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap.*;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import java.util.*;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AuthMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    var elasticsearchCfg = context.getElasticsearch();
    if (elasticsearchCfg.isEmpty()) {
      // TODO: Is this the correct reporter level?
      reporter.problem("Skipping auth migration: no elasticsearch configuration provided");
      return List.of();
    }

    var authc = elasticsearchCfg.get().security().get().authc();
    if (authc.get().isEmpty()) {
      return List.of();
    }

    var realms = authc.getValue().realms().get();

    var sortedRealms =
        realms.values().stream()
            .flatMap(
                realmType ->
                    realmType.get().values().stream()
                        .map(realm -> Map.entry(realm.get().order(), realm)))
            .sorted(Comparator.comparingInt(o -> o.getKey().get()))
            .map(Map.Entry::getValue)
            .toList();

    var authcDomains = new ImmutableList.Builder<SgAuthC.AuthDomain<?>>(sortedRealms.size());
    var frontendAuthcDomains =
        new ImmutableList.Builder<SgFrontendAuthC.AuthDomain<?>>(sortedRealms.size());

    for (var realm : sortedRealms) {
      if (realm.get() instanceof Realm.NativeRealm || realm.get() instanceof Realm.FileRealm) {
        authcDomains.add(new SgAuthC.AuthDomain.Internal());
      } else if (realm.get() instanceof Realm.LdapRealm ldapRealm) {
        authcDomains.add(migrateLdapRealm(Traceable.of(realm.getSource(), ldapRealm), reporter));
      } else if (realm.get() instanceof Realm.ActiveDirectoryRealm adRealm) {
        authcDomains.add(migrateActiveDirectoryRealm(Traceable.of(realm.getSource(), adRealm)));
      } else if (realm.get() instanceof Realm.SAMLRealm samlRealm) {
        frontendAuthcDomains.add(
            migrateSamlRealm(Traceable.of(realm.getSource(), samlRealm), reporter));
      } else {
        reporter.critical(realm, "Unrecognized realm type");
        return List.of();
      }
    }

    var results = new ArrayList<NamedConfig<?>>();

    var builtAuthcDomains = authcDomains.build();
    if (!builtAuthcDomains.isEmpty()) {
      results.add(new SgAuthC(builtAuthcDomains));
    }

    var builtFrontendAuthcDomains = frontendAuthcDomains.build();
    if (!builtFrontendAuthcDomains.isEmpty()) {
      results.add(new SgFrontendAuthC(builtFrontendAuthcDomains));
    }

    return results;
  }

  private SgAuthC.AuthDomain<?> migrateLdapRealm(
      Traceable<Realm.LdapRealm> realm, MigrationReporter reporter) {
    var realmUntraced = realm.get();
    var hosts = ImmutableList.of(realmUntraced.url().get().stream().map(Traceable::get).toList());
    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.empty(),
            realmUntraced.bindDn().get(),
            realmUntraced.secureBindPassword().get(),
            Optional.empty(),
            Optional.empty());

    var userSearch =
        Optional.of(
            new UserSearch(
                realmUntraced.userSearchBaseDn().get(),
                migrateSearchScope(realmUntraced.userSearchScope(), reporter),
                Optional.of(new Filter.Raw(realmUntraced.userSearchFilter().get()))));

    var groupSearch =
        realmUntraced
            .groupSearchBaseDn()
            .get()
            .map(
                groupSearchBaseDn ->
                    new GroupSearch(
                        groupSearchBaseDn,
                        migrateSearchScope(realmUntraced.groupSearchScope(), reporter),
                        Optional.empty(),
                        Optional.empty()));

    return new Ldap(identityProvider, userSearch, groupSearch);
  }

  private SgAuthC.AuthDomain<?> migrateActiveDirectoryRealm(
      Traceable<Realm.ActiveDirectoryRealm> realm) {
    var realmUntraced = realm.get();

    var hosts =
        realmUntraced
            .url()
            .get()
            .map(urls -> ImmutableList.of(urls.stream().map(Traceable::get).toList()))
            .orElse(ImmutableList.of("ldap://%s:389".formatted(realmUntraced.domainName().get())));

    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.empty(),
            realmUntraced.bindDn().get(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    var userSearch =
        Optional.of(
            new UserSearch(
                realmUntraced.userSearchBaseDn().get(), Optional.empty(), Optional.empty()));
    return new Ldap(identityProvider, userSearch, Optional.empty());
  }

  private SgFrontendAuthC.AuthDomain<?> migrateSamlRealm(
      Traceable<Realm.SAMLRealm> realm, MigrationReporter reporter) {
    var saml = realm.get();

    // idpMetadataPath is required for migration
    var metadataUrl =
        saml.idpMetadataPath()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(
                      realm, "SAML realm missing idp.metadata.path - using empty string");
                  return "";
                });

    // idpEntityId is required for migration
    var idpEntityId =
        saml.idpEntityId()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(realm, "SAML realm missing idp.entity_id - using empty string");
                  return "";
                });

    // spEntityId is required for migration
    var spEntityId =
        saml.spEntityId()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(realm, "SAML realm missing sp.entity_id - using empty string");
                  return "";
                });

    // Extract subject_key from attributes.principal (X-Pack) -> user_mapping.user_name.from (SG)
    var subjectKey = saml.attributesPrincipal().get();

    // Extract roles_key from attributes.groups (X-Pack) ->
    // user_mapping.roles.from_comma_separated_string (SG)
    var rolesKey = saml.attributesGroups().get();

    // Derive kibana_url from sp.acs (the ACS endpoint contains the Kibana URL)
    // X-Pack sp.acs is typically like "https://kibana.example.com/api/security/saml/callback"
    // We extract the base URL for Search Guard's kibana_url
    var kibanaUrl = saml.spAcs().get().map(AuthMigrator::extractKibanaUrlFromAcs);

    // Note: Search Guard requires an 'exchange_key' for JWT token signing.
    // This must be configured manually in sg_frontend_authc.yml as it is a new secret
    // not present in X-Pack configuration. Users should generate a secure random string (32+
    // chars).

    return new SgFrontendAuthC.AuthDomain.Saml(
        Optional.empty(), // label - use default
        Optional.of(saml.name().get()), // id - use realm name
        false, // isDefault
        metadataUrl,
        idpEntityId,
        spEntityId,
        subjectKey,
        rolesKey,
        kibanaUrl);
  }

  /**
   * Extracts the Kibana base URL from an ACS endpoint URL.
   *
   * <p>X-Pack sp.acs is typically like "https://kibana.example.com:5601/api/security/saml/callback"
   * This extracts "https://kibana.example.com:5601/" for Search Guard's kibana_url.
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

  private Optional<SearchScope> migrateSearchScope(
      Traceable<Realm.LdapRealm.Scope> scope, MigrationReporter reporter) {
    return switch (scope.get()) {
      case SUB_TREE -> Optional.of(SearchScope.SUB);
      case ONE_LEVEL -> Optional.of(SearchScope.ONE);
      case BASE -> {
        reporter.inconvertible(scope, "Cannot convert search scope as no equivalent scope exists");
        yield Optional.empty();
      }
    };
  }
}
