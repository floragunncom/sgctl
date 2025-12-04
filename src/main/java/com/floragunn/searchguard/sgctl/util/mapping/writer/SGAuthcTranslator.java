package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.google.common.collect.ImmutableMap;

import java.util.*;

public class SGAuthcTranslator {

    public static class Configs{
        public MigrateConfig.SgAuthc config;
        public MigrateConfig.SgAuthc fconfig;

        public Configs(MigrateConfig.SgAuthc config, MigrateConfig.SgAuthc fconfig){
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
        MigrateConfig.SgAuthc fconfig = new MigrateConfig.SgAuthc();
        config.authDomains = new ArrayList<>();


        fconfig.authDomains = new ArrayList<>();
        fconfig.internalProxies = "";
        fconfig.remoteIpHeader = "";

        ir.getAuthent().getRealms().forEach((String realmName, RealmIR realm) -> {
            String type = realm.getType();
            String keyPrefix = "xpack.security.authc.realms." + type + "." + realmName;
            MigrateConfig.NewAuthDomain newDomain = null;
            boolean isFrontendRealm = false;

            switch (type) {
                case "ldap":
                    newDomain = createLdapDomain(realmName, (RealmIR.LdapRealmIR) realm);
                    break;
                case "file":
                    newDomain = createFileDomain(realmName, (RealmIR.FileRealmIR) realm);
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "File realm migration not yet implemented.");
                    break;
                case "native":
                    newDomain = createNativeDomain(realmName, (RealmIR.NativeRealmIR) realm);
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "Native realm migration not yet implemented.");
                    break;
                case "saml":
                    newDomain = createSAMLDomain(realmName, (RealmIR.SamlRealmIR) realm);
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "SAML realm migration not yet implemented.");
                    break;
                case "pki":
                    newDomain = createPkiDomain(realmName, (RealmIR.PkiRealmIR) realm);
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "PKI realm migration not yet implemented.");
                    break;
                case "oidc":
                    newDomain = createOidcDomain(realmName, (RealmIR.OidcRealmIR) realm);
                    isFrontendRealm = true;
                    break;
                case "kerberos":
                    newDomain = createkerebosDomain(realmName, (RealmIR.KerberosRealmIR) realm);
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "Kereberos realm migration not yet implemented.");
                    break;
                default:
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, String.format("Encountered Unknown Realm (Name: %s, Type: %s)", realmName, type));
                    break;
            }

            if (newDomain != null) {
                if (isFrontendRealm) {
                    fconfig.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated("elasticsearch.yml", keyPrefix, "Realm migrated to sg_frontend_authc.yml");
                } else {
                    config.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated("elasticsearch.yml", keyPrefix, "Realm migrated to sg_authc.yml");
                }
            }

        });

        return new Configs(config, fconfig);
    }

    /**
     * Optionally adds a value to a config if value is not null
     * @param config The config that the value gets added to
     * @param key The Key which needs to be added
     * @param value Optional value that gets added if present
     */
    private static void addOptionalConfigProperty(Map<String, Object> config, String key, Object value) {
        if (value == null)
            return;
        config.put(key, value);
        MigrationReport.shared.addMigrated("sg_authc.yml", key);
    }

    /**
     * Creates the LDAP-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createLdapDomain(String realmName, RealmIR.LdapRealmIR ir) {
        Map<String, Object> ldapConfig = new HashMap<>();
        String url = ir.getUrl();
        if (url != null) {
            List<String> ldapHosts = Arrays.asList(url);
            ldapConfig.put("ldap.idp.hosts", ldapHosts);
        }
        addOptionalConfigProperty(ldapConfig, "ldap.idp.bind_dn", ir.getBindDn());
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.base_dn", ir.getUserSearchBaseDn());
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.raw", ir.getUserSearchFilter());
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.base_dn", ir.getGroupSearchBaseDn());

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
        addOptionalConfigProperty(oidcConfig, "rp.client_id", ir.getRpClientId());
        addOptionalConfigProperty(oidcConfig, "rp.response_type", ir.getRpResponseType());
        // 2. OP settings
        addOptionalConfigProperty(oidcConfig, "op.issuer", ir.getOpIssuer());
        addOptionalConfigProperty(oidcConfig, "op.authorization_endpoint", ir.getOpAuthEndpoint());
        addOptionalConfigProperty(oidcConfig, "op.token_endpoint", ir.getOpTokenEndpoint());
        addOptionalConfigProperty(oidcConfig, "op.jwkset_path", ir.getOpJwkSetPath());
        addOptionalConfigProperty(oidcConfig, "claims.principal", ir.getClaimPrincipal());

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
