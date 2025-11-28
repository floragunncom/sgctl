package com.floragunn.searchguard.sgctl.config.migrate.auth;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgConfig.*;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig.Realm;
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
      } else {
        throw new UnsupportedOperationException();
      }

      authcDomains.put(name, domain);
    }

    return new SgConfig(
        ImmutableMap.empty(),
        new SearchGuard(
            false,
            "",
            new Dynamic(
                "",
                null,
                "",
                null,
                null,
                new Kibana(false, null, null),
                new Http(false, null),
                new AuthTokenProvider(false, null, ImmutableMap.empty()),
                authcDomains.build(),
                ImmutableMap.empty())));
  }

  private SgConfig.AuthcDomain migrateBasicRealm(Realm realm) {
    return new SgConfig.AuthcDomain(
        realm.enabled(),
        realm.enabled(),
        realm.order(),
        new HttpAuthenticator.Basic(true),
        new AuthenticationBackend.Internal(),
        new AuthorizationBackend.Noop());
  }

  private SgConfig.AuthcDomain migrateLdapRealm(Realm.LdapRealm realm) {
    return new SgConfig.AuthcDomain(
        realm.enabled(),
        realm.enabled(),
        realm.order(),
        new HttpAuthenticator.Basic(true),
        new AuthenticationBackend.Ldap(
            true,
            false,
            false,
            true,
            realm.url(),
            realm.bindDn(),
            "", // TODO: password,
            realm.userSearchBaseDn(),
            realm.userSearchFilter(),
            "" // TODO: usernameAttribute
            ),
        new AuthorizationBackend.Ldap(
            true,
            false,
            false,
            true,
            realm.url(),
            realm.bindDn(),
            "", // TODO: password
            realm.userSearchBaseDn(),
            realm.userSearchFilter(),
            "", // TODO: usernameAttribute
            realm.groupSearchBaseDn(),
            "", // TODO: roleSearch
            null,
            "none", // TODO maybe ?
            "", // TODO
            true,
            ImmutableList.empty()));
  }
  
}
