package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.*;

public class SGAuthcTranslator {
    /**
     * Creates Authc Config
     *
     * @param ir The intermediate representation.
     * @return Populated SgAuthc object.
     */
    public static MigrateConfig.SgAuthc createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {
        MigrateConfig.SgAuthc config = new MigrateConfig.SgAuthc();
        config.authDomains = new ArrayList<>();
        config.internalProxies = "";
        config.remoteIpHeader = "";
        ir.authent.realms.forEach((String realmName, RealmIR realm) -> {
            String type = realm.getType();
            //TODO Add Migration Report note on translated realm
            switch (type) {
                case "ldap":
                    config.authDomains.add(createLdapDomain(realmName, (RealmIR.LdapRealmIR) realm));
                    break;
                case "file":
                    config.authDomains.add(createFileDomain(realmName, (RealmIR.FileRealmIR) realm));
                    break;
                case "native":
                    config.authDomains.add(createNativeDomain(realmName, (RealmIR.NativeRealmIR) realm));
                    break;
                case "saml":
                    config.authDomains.add(createSAMLDomain(realmName, (RealmIR.SamlRealmIR) realm));
                    break;
                case "pki":
                    config.authDomains.add(createPkiDomain(realmName, (RealmIR.PkiRealmIR) realm));
                    break;
                case "oidc":
                    config.authDomains.add(createOidcDomain(realmName, (RealmIR.OidcRealmIR) realm));
                    break;
                case "kerberos":
                    config.authDomains.add(createkerebosDomain(realmName, (RealmIR.KerberosRealmIR) realm));
                    break;
                default:
                    //Skip Unknown Realms
                    //TODO add Migration Report Note
                    break;
            }
        });

        return config;
    }

    /**
     * Creates the LDAP-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createLdapDomain(String realmName, RealmIR.LdapRealmIR ir) {
        Map<String, Object> ldapConfig = new HashMap<>();

        List<String> ldapHosts = Arrays.asList(ir.getUrl());
        ldapConfig.put("ldap.idp.hosts", ldapHosts);
        ldapConfig.put("ldap.idp.bind_dn", ir.getBindDn());
        ldapConfig.put("ldap.user_search.base_dn", ir.getUserSearchBaseDn());
        ldapConfig.put("ldap.user_search.filter.raw", ir.getUserSearchFilter());
        ldapConfig.put("ldap.group_search.base_dn", ir.getGroupSearchBaseDn());

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                ldapConfig,
                null
        );
    }
    //TODO Implement these functions. They are just place holders for now
    private static MigrateConfig.NewAuthDomain createFileDomain(String realmName, RealmIR.FileRealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createNativeDomain(String realmName, RealmIR.NativeRealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createSAMLDomain(String realmName, RealmIR.SamlRealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createPkiDomain(String realmName, RealmIR.PkiRealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createkerebosDomain(String realmName, RealmIR.KerberosRealmIR ir) {
        return null;
    }

    /**
     * Creates the OIDC-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createOidcDomain(String realmName, RealmIR.OidcRealmIR ir) {
        Map<String, Object> oidcConfig = new HashMap<>();


        return new MigrateConfig.NewAuthDomain(
                "oidc",
                null,
                null,
                null,
                oidcConfig,
                null
        );
    }
}
