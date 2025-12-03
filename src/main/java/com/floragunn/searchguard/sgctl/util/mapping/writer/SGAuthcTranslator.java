package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.commands.MigrateSecurity;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.google.common.collect.ImmutableMap;

import java.util.*;

public class SGAuthcTranslator {

    public static class Configs{
        public MigrateConfig.SgAuthc config;
        public SgFrontEndAuthc fconfig;

        public Configs(MigrateConfig.SgAuthc config, SgFrontEndAuthc fconfig){
            this.config = config;
            this.fconfig = fconfig;
        }

    }
    /**
     * Creates Authc Config
     *
     * @param ir The intermediate representation.
     * @return Populated SgAuthc object.
     */
    public static Configs createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {
        MigrateConfig.SgAuthc config = new MigrateConfig.SgAuthc();
        SgFrontEndAuthc fconfig = new SgFrontEndAuthc();
        config.authDomains = new ArrayList<>();


        fconfig.authDomains = new ArrayList<>();
        fconfig.internalProxies = "";
        fconfig.remoteIpHeader = "";

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
                    //This case do not belong in sg_authc.yml but sg_frontend_authc.yml. (Could be the same with other realms)
                    fconfig.authDomains.add(createOidcDomain(realmName, (RealmIR.OidcRealmIR) realm));
                    break;
                case "kerberos":
                    config.authDomains.add(createkerebosDomain(realmName, (RealmIR.KerberosRealmIR) realm));
                    break;
                default:
                    System.out.println("Invalid option");
                    //Skip Unknown Realms
                    //TODO add Migration Report Note
                    break;
            }

        });

        return new Configs(config, fconfig);
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
        Map<String, Object> samlConfig = new HashMap<>();

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                samlConfig,
                null
        );
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

        // 1. RP settings
        if (ir.getRpClientId() != null)
            oidcConfig.put("rp.client_id", ir.getRpClientId());
        if (ir.getRpResponseType() != null)
            oidcConfig.put("rp.response_type", ir.getRpResponseType());
        // 2. OP settings
        if (ir.getOpIssuer() != null)
            oidcConfig.put("op.issuer", ir.getOpIssuer());
        if (ir.getOpAuthEndpoint() != null)
            oidcConfig.put("op.authorization_endpoint", ir.getOpAuthEndpoint());
        if (ir.getOpTokenEndpoint() != null)
            oidcConfig.put("op.token_endpoint", ir.getOpTokenEndpoint());
        if (ir.getOpJwkSetPath() != null)
            oidcConfig.put("op.jwkset_path", ir.getOpJwkSetPath());

        if (ir.getClaimPrincipal() != null)
            oidcConfig.put("claims.principal", ir.getClaimPrincipal());

        return new MigrateConfig.NewAuthDomain(
                "oidc",
                null,
                null,
                null,
                oidcConfig,
                null
        );
    }

    public static class SgFrontEndAuthc extends MigrateConfig.SgAuthc implements Document<MigrateConfig.SgAuthc> {

        @Override
        public Object toBasicObject() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("auth_domains", authDomains);

            if (internalProxies != null || remoteIpHeader != null) {

                Map<String, Object> network = new LinkedHashMap<>();

                if (internalProxies != null) {
                    network.put("trusted_proxies_regex", internalProxies);
                }

                if (remoteIpHeader != null) {
                    network.put("http", ImmutableMap.of("remote_ip_header", remoteIpHeader));
                }

                result.put("network", network);
            }

            return result;
        }

    }
}
