package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain.Ldap.*;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class AuthMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
      throws SgctlException {
    var elasticsearchCfg = context.getElasticsearch();
    if (elasticsearchCfg.isEmpty()) {
      logger.debug("Skipping auth migration: no elasticsearch configuration provided");
      return List.of();
    }

    var realms = elasticsearchCfg.get().security().authc().realms();

    var authcDomains = new ImmutableList.Builder<SgAuthC.AuthDomain<?>>(realms.size());
    for (var realm : realms.values()) {
      SgAuthC.AuthDomain<?> domain;
      if (realm instanceof Realm.NativeRealm || realm instanceof Realm.FileRealm) {
        domain = new SgAuthC.AuthDomain.Internal();
      } else if (realm instanceof Realm.LdapRealm ldapRealm) {
        domain = migrateLdapRealm(ldapRealm, logger);
      } else if (realm instanceof Realm.ActiveDirectoryRealm adRealm) {
        domain = migrateActiveDirectoryRealm(adRealm);
      } else {
        throw new UnsupportedOperationException();
      }

      authcDomains.add(domain);
    }

    return List.of(new SgAuthC(authcDomains.build()));
  }

  private SgAuthC.AuthDomain<?> migrateLdapRealm(Realm.LdapRealm realm, Logger logger) {
    var identityProvider =
        new IdentityProvider(
            realm.url(),
            Optional.empty(),
            Optional.ofNullable(realm.bindDn()),
            Optional.ofNullable(realm.secureBindPassword()),
            Optional.empty(),
            Optional.empty());

    var userSearch =
        Optional.of(
            new UserSearch(
                Optional.ofNullable(realm.userSearchBaseDn()),
                migrateSearchScope(realm.userSearchScope(), logger),
                Optional.ofNullable(realm.userSearchFilter()).map(Filter.Raw::new)));

    var groupSearch =
        realm.groupSearchBaseDn() == null
            ? Optional.<GroupSearch>empty()
            : Optional.of(
                new GroupSearch(
                    realm.groupSearchBaseDn(),
                    migrateSearchScope(realm.groupSearchScope(), logger),
                    Optional.empty(),
                    Optional.empty()));

    return new Ldap(identityProvider, userSearch, groupSearch);
  }

  private SgAuthC.AuthDomain<?> migrateActiveDirectoryRealm(Realm.ActiveDirectoryRealm realm) {
    var identityProvider =
        new IdentityProvider(
            realm.url(),
            Optional.empty(),
            Optional.ofNullable(realm.bindDn()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    var userSearch =
        Optional.of(
            new UserSearch(
                Optional.ofNullable(realm.userSearchBaseDn()), Optional.empty(), Optional.empty()));
    return new Ldap(identityProvider, userSearch, Optional.empty());
  }

  private Optional<SearchScope> migrateSearchScope(Realm.LdapRealm.Scope scope, Logger logger) {
    if (scope == null) return Optional.empty();
    return switch (scope) {
      case SUB_TREE -> Optional.of(SearchScope.SUB);
      case ONE_LEVEL -> Optional.of(SearchScope.ONE);
      case BASE -> {
        logger.warn("Cannot convert search scope '{}' as no equivalent scope exists", scope.name());
        yield Optional.empty();
      }
    };
  }
}
