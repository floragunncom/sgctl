package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap.*;
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

    for (var realm : sortedRealms) {
      // Report if enabled field was missing (default was applied)
      if (!realm.get().enabled().isPresent()) {
        reporter.defaultApplied(realm, "enabled", "true");
      }

      if (realm.get() instanceof Realm.NativeRealm || realm.get() instanceof Realm.FileRealm) {
        authcDomains.add(new SgAuthC.AuthDomain.Internal());
      } else if (realm.get() instanceof Realm.LdapRealm) {
        authcDomains.add(migrateLdapRealm(realm.map(r -> (Realm.LdapRealm) r), reporter));
      } else if (realm.get() instanceof Realm.ActiveDirectoryRealm) {
        authcDomains.add(
            migrateActiveDirectoryRealm(realm.map(r -> (Realm.ActiveDirectoryRealm) r), reporter));
      } else if (realm.get() instanceof Realm.SAMLRealm) {
        // SAML realms are handled by FrontendAuthMigrator - skip here
        continue;
      } else if (realm.get() instanceof Realm.GenericRealm genericRealm
          && "oidc".equals(genericRealm.type().get())) {
        // OIDC realms are handled by FrontendAuthMigrator - skip here
        continue;
      } else {
        reporter.problem(realm, "Skipping unrecognized realm type: " + realm.get().type().get());
        continue;
      }
    }

    var builtAuthcDomains = authcDomains.build();
    if (builtAuthcDomains.isEmpty()) {
      return List.of();
    }

    return List.of(new SgAuthC(builtAuthcDomains));
  }

  private SgAuthC.AuthDomain<?> migrateLdapRealm(
      Traceable<Realm.LdapRealm> realm, MigrationReporter reporter) {

    reportIfConnectionPoolDisabled(realm.get().userSearchPoolEnabled(), reporter);

    // Validate that LDAP URL is present and not empty
    var urlList = realm.get().url().get();
    if (urlList.isEmpty()) {
      reporter.critical(
          realm,
          "LDAP realm '"
              + realm.get().name().get()
              + "' is missing required field 'url'. "
              + "LDAP authentication cannot function without a server URL. "
              + "Please specify the LDAP server URL (e.g., url: \"ldap://localhost:389\") in elasticsearch.yml.");
      return new Ldap(
          new IdentityProvider(
              ImmutableList.empty(),
              Optional.of(IdentityProvider.ConnectionStrategy.FAILOVER),
              Optional.empty(),
              Optional.empty(),
              Optional.of(0),
              Optional.of(20)),
          Optional.empty(),
          Optional.empty());
    }

    var hosts = ImmutableList.of(urlList.stream().map(Traceable::get).toList());
    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.of(migrateLoadBalanceType(realm.get().loadBalanceType(), reporter)),
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
                  Optional.of(normalizeDn(realm.get().userSearchBaseDn().getValue())),
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
                  normalizeDn(realm.get().groupSearchBaseDn().getValue()),
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

    // Validate that either URL or domainName is present
    var urlOptional = realm.get().url().get();
    var domainName = realm.get().domainName().get();

    if (urlOptional.isEmpty() && domainName.isBlank()) {
      reporter.critical(
          realm,
          "Active Directory realm '"
              + realm.get().name().get()
              + "' is missing both 'url' and 'domainName'. "
              + "At least one must be specified for Active Directory authentication. "
              + "Please specify either url (e.g., url: \"ldap://dc.example.com:389\") "
              + "or domainName (e.g., domainName: \"example.com\") in elasticsearch.yml.");
      return new Ldap(
          new IdentityProvider(
              ImmutableList.empty(),
              Optional.of(IdentityProvider.ConnectionStrategy.FAILOVER),
              Optional.empty(),
              Optional.empty(),
              Optional.of(0),
              Optional.of(20)),
          Optional.empty(),
          Optional.empty());
    }

    var hosts =
        urlOptional
            .map(urls -> ImmutableList.of(urls.stream().map(Traceable::get).toList()))
            .orElse(ImmutableList.of("ldap://%s:389".formatted(domainName)));

    var identityProvider =
        new IdentityProvider(
            hosts,
            Optional.of(migrateLoadBalanceType(realm.get().loadBalanceType(), reporter)),
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
      reporter.problem(
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

  private IdentityProvider.ConnectionStrategy migrateLoadBalanceType(
      Traceable<Realm.LoadBalanceType> type, MigrationReporter reporter) {
    return switch (type.get()) {
      case FAILOVER -> IdentityProvider.ConnectionStrategy.FAILOVER;
      case ROUND_ROBIN -> IdentityProvider.ConnectionStrategy.ROUNDROBIN;
      case DNS_FAILOVER -> {
        reporter.problem(type, "dns_failover mode does not exist, using failover instead");
        yield IdentityProvider.ConnectionStrategy.FAILOVER;
      }
      case DNS_ROUND_ROBIN -> {
        reporter.problem(type, "dns_round_robin mode does not exist, using roundrobin instead");
        yield IdentityProvider.ConnectionStrategy.ROUNDROBIN;
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
   * Normalizes LDAP DNs by removing spaces after commas. X-Pack allows "ou=users, o=services,
   * dc=example, dc=com" Search Guard expects "ou=users,o=services,dc=example,dc=com"
   */
  private String normalizeDn(String dn) {
    return dn.replaceAll(",\\s*", ",");
  }
}
