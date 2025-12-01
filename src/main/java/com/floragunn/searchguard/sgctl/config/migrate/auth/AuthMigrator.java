package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import java.util.Optional;
import org.slf4j.Logger;

public class AuthMigrator {

  public SgAuthC migrate(XPackElasticsearchConfig xpack, Logger logger) {
    var realms = xpack.security().authc().realms();

    var authcDomains = new ImmutableList.Builder<SgAuthC.AuthDomain<?>>(realms.size());
    for (var realmEntry : realms.entrySet()) {
      var name = realmEntry.getKey();
      var realm = realmEntry.getValue();

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

    return new SgAuthC(authcDomains.build());
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
