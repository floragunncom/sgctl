package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap.*;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AuthMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    var elasticsearchCfg = context.getElasticsearch();
    if (elasticsearchCfg.isEmpty()) {
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
        authcDomains.add(
            migrateActiveDirectoryRealm(Traceable.of(realm.getSource(), adRealm), reporter));
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

    reportIfConnectionPoolDisabled(realm.get().userSearchPoolEnabled(), reporter);

    var hosts = ImmutableList.of(realm.get().url().get().stream().map(Traceable::get).toList());
    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.empty(),
            realm.get().bindDn().get(),
            takeBindOrSecureBindPassword(
                realm.get().bindPassword(), realm.get().secureBindPassword(), reporter),
            Optional.of(realm.get().userSearchPoolInitialSize().get()),
            Optional.of(realm.get().userSearchPoolSize().get()));

    Optional<UserSearch> userSearch;
    if (realm
        .get()
        .userSearchBaseDn()
        .isPresent()) { // "X-Pack: Required to operated in user search mode."
      userSearch =
          Optional.of(
              new UserSearch(
                  realm.get().userSearchBaseDn().get(),
                  migrateSearchScope(realm.get().userSearchScope(), reporter),
                  Optional.of(migrateUserSearchFilter(realm.get().userSearchFilter()))));
    } else {
      userSearch = Optional.empty();
    }

    Optional<GroupSearch> groupSearch;
    if (realm.get().groupSearchBaseDn().get().isPresent()) {
      groupSearch =
          Optional.of(
              new GroupSearch(
                  realm.get().groupSearchBaseDn().getValue(),
                  migrateSearchScope(realm.get().groupSearchScope(), reporter),
                  Optional.empty(),
                  Optional.empty()));
    } else {
      reporter.critical(
          realm.get().groupSearchBaseDn(),
          "Config entry not specified; defaulting behavior not implemented in migrator");
      groupSearch = Optional.empty();
    }

    return new Ldap(identityProvider, userSearch, groupSearch);
  }

  private SgAuthC.AuthDomain<?> migrateActiveDirectoryRealm(
      Traceable<Realm.ActiveDirectoryRealm> realm, MigrationReporter reporter) {

    reportIfConnectionPoolDisabled(realm.get().userSearchPoolEnabled(), reporter);

    var hosts =
        realm
            .get()
            .url()
            .get()
            .map(urls -> ImmutableList.of(urls.stream().map(Traceable::get).toList()))
            .orElse(ImmutableList.of("ldap://%s:389".formatted(realm.get().domainName().get())));

    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.empty(),
            realm.get().bindDn().get(),
            takeBindOrSecureBindPassword(
                realm.get().bindPassword(), realm.get().secureBindPassword(), reporter),
            Optional.of(realm.get().userSearchPoolInitialSize().get()),
            Optional.of(realm.get().userSearchPoolSize().get()));

    var userSearchBaseDn =
        realm
            .get()
            .userSearchBaseDn()
            .get()
            .orElseGet(() -> convertDomainToDn(realm.get().domainName().get()));

    var groupSearchBaseDn =
        realm
            .get()
            .groupSearchBaseDn()
            .get()
            .orElseGet(() -> convertDomainToDn(realm.get().domainName().get()));

    var userSearch =
        new UserSearch(
            Optional.of(userSearchBaseDn),
            migrateSearchScope(realm.get().userSearchScope(), reporter),
            Optional.of(migrateUserSearchFilter(realm.get().userSearchFilter())));

    var groupSearch =
        new GroupSearch(
            groupSearchBaseDn,
            migrateSearchScope(realm.get().groupSearchScope(), reporter),
            Optional.empty(),
            Optional.empty());

    return new Ldap(identityProvider, Optional.of(userSearch), Optional.of(groupSearch));
  }

  private void reportIfConnectionPoolDisabled(
      Traceable<Boolean> enabled, MigrationReporter reporter) {
    if (enabled.get()) return;
    reporter.inconvertible(enabled, "Connection pool cannot be disabled in Search Guard");
  }

  private Filter.Raw migrateUserSearchFilter(Traceable<String> filter) {
    return new Filter.Raw(filter.get().replace("{{0}}", "${user.name}"));
  }

  private Optional<String> takeBindOrSecureBindPassword(
      Traceable<String> bindPassword,
      Traceable<String> secureBindPassword,
      MigrationReporter reporter) {
    if (bindPassword.get().isBlank() && !secureBindPassword.get().isBlank())
      return Optional.of(secureBindPassword.get());
    if (!bindPassword.get().isBlank() && secureBindPassword.get().isBlank())
      return Optional.of(bindPassword.get());
    if (!bindPassword.get().isBlank() && !secureBindPassword.get().isBlank()) {
      reporter.problemSecret(
          bindPassword,
          "Both bind_password and secure_bind_password are set; using secure_bind_password");
      return Optional.of(secureBindPassword.get());
    }
    return Optional.empty();
  }

  private Optional<SearchScope> migrateSearchScope(
      Traceable<Realm.SearchScope> scope, MigrationReporter reporter) {
    return switch (scope.get()) {
      case SUB_TREE -> Optional.of(SearchScope.SUB);
      case ONE_LEVEL -> Optional.of(SearchScope.ONE);
      case BASE -> {
        var availableSearchScopesMsg =
            Arrays.stream(SearchScope.values())
                .map(SearchScope::name)
                .collect(Collectors.joining(", "));
        reporter.inconvertible(
            scope,
            "These other migratable search scopes DO exist in Search Guard: "
                + availableSearchScopesMsg
                + ". The search scope was omitted from the output because of this.");
        yield Optional.empty();
      }
    };
  }

  private String convertDomainToDn(String domainName) {
    if (domainName.isBlank())
      throw new IllegalArgumentException("Domain name cannot be null or empty");

    return Arrays.stream(domainName.split("\\."))
        .map(component -> "DC=" + component)
        .collect(Collectors.joining(","));
  }

  /**
   * Migrates X-Pack SAML realm to Search Guard frontend authc SAML domain.
   *
   * <p>Maps the following fields:
   *
   * <ul>
   *   <li>idp.metadata.path → saml.idp.metadata_url
   *   <li>idp.entity_id → saml.idp.entity_id
   *   <li>sp.entity_id → saml.sp.entity_id
   *   <li>attributes.principal → user_mapping.user_name.from
   *   <li>attributes.groups → user_mapping.roles.from_comma_separated_string
   *   <li>sp.acs → kibana_url (derived)
   * </ul>
   */
  private SgFrontendAuthC.AuthDomain.Saml migrateSamlRealm(
      Traceable<Realm.SAMLRealm> realm, MigrationReporter reporter) {
    var saml = realm.get();

    // idpMetadataPath is required for migration
    var metadataUrl =
        saml.idpMetadataPath()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(
                      saml.idpMetadataPath(), "SAML realm missing value - using empty string");
                  return "";
                });

    // idpEntityId is required for migration
    var idpEntityId =
        saml.idpEntityId()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(
                      saml.idpEntityId(), "SAML realm missing value - using empty string");
                  return "";
                });

    // spEntityId is required for migration
    var spEntityId =
        saml.spEntityId()
            .get()
            .orElseGet(
                () -> {
                  reporter.problem(
                      saml.spEntityId(), "SAML realm missing value - using empty string");
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
  static String extractKibanaUrlFromAcs(String acsUrl) {
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
