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
      SgAuthC.AuthDomain<?> domain;
      if (realm.get() instanceof Realm.NativeRealm || realm.get() instanceof Realm.FileRealm) {
        domain = new SgAuthC.AuthDomain.Internal();
      } else if (realm.get() instanceof Realm.LdapRealm ldapRealm) {
        domain = migrateLdapRealm(Traceable.of(realm.getSource(), ldapRealm), reporter);
      } else if (realm.get() instanceof Realm.ActiveDirectoryRealm adRealm) {
        domain = migrateActiveDirectoryRealm(Traceable.of(realm.getSource(), adRealm), reporter);
      } else {
        reporter.critical(realm, "Unrecognized realm type");
        return List.of();
      }

      authcDomains.add(domain);
    }

    return List.of(new SgAuthC(authcDomains.build()));
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
        reporter.inconvertible(scope, "Cannot convert search scope as no equivalent scope exists");
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
}
