package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgConfig.*;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
import java.util.Optional;
import org.slf4j.Logger;

public class AuthMigrator {

  public SgConfig migrate(XPackElasticsearchConfig xpack, Logger logger) {
    var realms = xpack.security().authc().realms();

    var authcDomains = new ImmutableMap.Builder<String, SgConfig.AuthcDomain>();
    for (var realmEntry : realms.entrySet()) {
      var name = realmEntry.getKey();
      var realm = realmEntry.getValue();

      SgConfig.AuthcDomain domain;
      if (realm instanceof Realm.NativeRealm || realm instanceof Realm.FileRealm) {
        domain = migrateBasicRealm(realm);
      } else if (realm instanceof Realm.LdapRealm ldapRealm) {
        domain = migrateLdapRealm(ldapRealm);
      } else if (realm instanceof Realm.ActiveDirectoryRealm adRealm) {
        domain = migrateActiveDirectoryRealm(adRealm);
      } else {
        throw new UnsupportedOperationException();
      }

      authcDomains.put(name, domain);
    }

    return new SgConfig(
        ImmutableMap.empty(),
        new SearchGuard(
            Optional.empty(),
            Optional.empty(),
            new Dynamic(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                authcDomains.build(),
                ImmutableMap.empty())));
  }

  private SgConfig.AuthcDomain migrateBasicRealm(Realm realm) {
    return new SgConfig.AuthcDomain(
        Optional.of(realm.enabled()),
        Optional.of(realm.enabled()),
        realm.order(),
        new HttpAuthenticator.Basic(true),
        new AuthenticationBackend.Internal());
  }

  private SgConfig.AuthcDomain migrateLdapRealm(Realm.LdapRealm realm) {
    return new SgConfig.AuthcDomain(
        Optional.of(realm.enabled()),
        Optional.of(realm.enabled()),
        realm.order(),
        new HttpAuthenticator.Basic(true),
        new AuthenticationBackend.Ldap(
            Optional.of(true),
            Optional.of(false),
            Optional.of(false),
            Optional.of(true),
            realm.url(),
            realm.bindDn(),
            "", // TODO: password,
            realm.userSearchBaseDn(),
            realm.userSearchFilter(),
            Optional.of("") // TODO: usernameAttribute
            ));
  }

  private SgConfig.AuthcDomain migrateActiveDirectoryRealm(Realm.ActiveDirectoryRealm realm) {
    return new SgConfig.AuthcDomain(
        Optional.of(realm.enabled()),
        Optional.of(realm.enabled()),
        realm.order(),
        new HttpAuthenticator.Basic(true),
        new AuthenticationBackend.Ldap(
            Optional.of(true),
            Optional.of(false),
            Optional.of(false),
            Optional.of(true),
            realm.url(),
            realm.bindDn(),
            "", // TODO: password,
            realm.userSearchBaseDn(),
            "", // realm.userSearchFilter(), TODO
            Optional.of("") // TODO: usernameAttribute
            ));
  }
}
