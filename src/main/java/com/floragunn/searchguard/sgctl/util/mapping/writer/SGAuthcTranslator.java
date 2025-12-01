package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.*;

public class SGAuthcTranslator {
    /**
     * Maps LDAP configuration from intermediate representation to Search Guard's sg_authc.yml format.
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
            switch (realm.getType()) {
                case "ldap":
                    config.authDomains.add(createLdapDomain(realmName, realm));
                    break;
                case "file":
                    config.authDomains.add(createFileDomain(realmName, realm));
                    break;
                case "native":
                    config.authDomains.add(createNativeDomain(realmName, realm));
                    break;
                case "saml":
                    config.authDomains.add(createSAMLDomain(realmName, realm));
                    break;
                case "pki":
                    config.authDomains.add(createPkiDomain(realmName, realm));
                    break;
                case "oidc":
                    config.authDomains.add(createOidcDomain(realmName, realm));
                    break;
                case "kerberos":
                    config.authDomains.add(createkerebosDomain(realmName, realm));
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
    private static MigrateConfig.NewAuthDomain createLdapDomain(String realmName, RealmIR ir) {
        Map<String, Object> ldapConfig = new HashMap<>();
        List<String> ldapHosts = Arrays.asList("ldap.example.com", "other.example.com");
        ldapConfig.put("ldap.idp.hosts", ldapHosts);

        return new MigrateConfig.NewAuthDomain(
                "basic/ldap",
                null,
                null,
                null,
                ldapConfig,
                null
        );
    }
    //TODO Implement these functions. They are just place holders for now
    private static MigrateConfig.NewAuthDomain createFileDomain(String realmName, RealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createNativeDomain(String realmName, RealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createSAMLDomain(String realmName, RealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createPkiDomain(String realmName, RealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createkerebosDomain(String realmName, RealmIR ir) {
        return null;
    }

    /**
     * Creates the OIDC-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createOidcDomain(String realmName, RealmIR ir) {
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
