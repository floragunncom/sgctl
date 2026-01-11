package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.migrate.util.MessageBuilder;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap.*;
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
    for (var realm : sortedRealms) {
      SgAuthC.AuthDomain<?> domain;
      if (realm.get() instanceof Realm.NativeRealm || realm.get() instanceof Realm.FileRealm) {
        domain = new SgAuthC.AuthDomain.Internal();
      } else if (realm.get() instanceof Realm.LdapRealm ldapRealm) {
        domain = migrateLdapRealm(Traceable.of(realm.getSource(), ldapRealm), reporter);
      } else if (realm.get() instanceof Realm.ActiveDirectoryRealm adRealm) {
        domain = migrateActiveDirectoryRealm(Traceable.of(realm.getSource(), adRealm));
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

  private void addBaseSearchScopeInconvertibleErrorMessage(
      Traceable<Realm.LdapRealm.Scope> scope, MigrationReporter reporter) {
    final SearchScope[] possibleScopes = SearchScope.values();
    final MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.appendKeyValueSeparated(
        "A different migratable search scope DOES exist in Search Guard",
        "These other migratable search scopes DO exist in Search Guard",
        possibleScopes,
        Enum::name);
    messageBuilder.nextSentence();
    messageBuilder.append("The search scope was omitted from the output because of this");
    messageBuilder.nextSentence();
    final String errorMessage = messageBuilder.finalizeMessage();
    reporter.inconvertible(scope, errorMessage);
  }

  private Optional<SearchScope> migrateSearchScope(
      Traceable<Realm.LdapRealm.Scope> scope, MigrationReporter reporter) {
    return switch (scope.get()) {
      case SUB_TREE -> Optional.of(SearchScope.SUB);
      case ONE_LEVEL -> Optional.of(SearchScope.ONE);
      case BASE -> {
        addBaseSearchScopeInconvertibleErrorMessage(scope, reporter);
        yield Optional.empty();
      }
    };
  }
}
