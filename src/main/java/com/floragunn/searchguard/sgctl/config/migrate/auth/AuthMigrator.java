package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
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
        domain = migrateBasicRealm(realm);
      } else if (realm instanceof Realm.LdapRealm ldapRealm) {
        domain = migrateLdapRealm(ldapRealm);
      } else if (realm instanceof Realm.ActiveDirectoryRealm adRealm) {
        domain = migrateActiveDirectoryRealm(adRealm);
      } else {
        throw new UnsupportedOperationException();
      }

      authcDomains.add(domain);
    }

    return List.of(new SgAuthC(authcDomains.build()));
  }

  private SgAuthC.AuthDomain<?> migrateBasicRealm(Realm realm) {
    return new SgAuthC.AuthDomain.Internal();
  }

  private SgAuthC.AuthDomain<?> migrateLdapRealm(Realm.LdapRealm realm) {
    var identityProvider =
        new SgAuthC.AuthDomain.Ldap.IdentityProvider(
            realm.url(),
            Optional.empty(),
            Optional.ofNullable(realm.bindDn()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var userSearch =
        Optional.of(
            new SgAuthC.AuthDomain.Ldap.UserSearch(
                Optional.ofNullable(realm.userSearchBaseDn()),
                Optional.empty(),
                Optional.ofNullable(realm.userSearchFilter())
                    .map(SgAuthC.AuthDomain.Ldap.Filter.Raw::new)));

    var groupSearch =
        realm.groupSearchBaseDn() == null
            ? Optional.<SgAuthC.AuthDomain.Ldap.GroupSearch>empty()
            : Optional.of(
                new SgAuthC.AuthDomain.Ldap.GroupSearch(
                    realm.groupSearchBaseDn(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));

    return new SgAuthC.AuthDomain.Ldap(identityProvider, userSearch, groupSearch);
  }

  private SgAuthC.AuthDomain<?> migrateActiveDirectoryRealm(Realm.ActiveDirectoryRealm realm) {
    var identityProvider =
        new SgAuthC.AuthDomain.Ldap.IdentityProvider(
            realm.url(),
            Optional.empty(),
            Optional.ofNullable(realm.bindDn()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    var userSearch =
        Optional.of(
            new SgAuthC.AuthDomain.Ldap.UserSearch(
                Optional.ofNullable(realm.userSearchBaseDn()), Optional.empty(), Optional.empty()));
    return new SgAuthC.AuthDomain.Ldap(identityProvider, userSearch, Optional.empty());
  }
}
