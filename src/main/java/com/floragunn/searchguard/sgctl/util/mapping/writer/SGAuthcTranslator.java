package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.*;

public class SGAuthcTranslator {
    private static final String SG_AUTHC_FILE_NAME = "sg_authc.yml";
    public static class Configs{
        public final MigrateConfig.SgAuthc config;
        public final MigrateConfig.SgAuthc fconfig;

        public Configs(MigrateConfig.SgAuthc config, MigrateConfig.SgAuthc fconfig){
            this.config = config;
            this.fconfig = fconfig;
        }

    }

    private SGAuthcTranslator() {}

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
            //Handle disabled realms, like discussed
            if (!realm.isEnabled()) {
                MigrationReport.shared.addIgnoredKey(SG_AUTHC_FILE_NAME, realmName, realm.getType());
                return;
            }
            String type = realm.getType();
            MigrateConfig.NewAuthDomain newDomain = null;
            boolean isFrontendRealm = false;

            switch (type) {
                case "ldap":
                    newDomain = createLdapDomain((RealmIR.LdapRealmIR) realm);
                    break;
                case "file":
                    newDomain = createFileDomain((RealmIR.FileRealmIR) realm);
                    realmNotImplementedReport(realmName, realm);
                    break;
                case "native":
                    newDomain = createNativeDomain((RealmIR.NativeRealmIR) realm);
                    realmNotImplementedReport(realmName, realm);
                    break;
                case "saml":
                    newDomain = createSAMLDomain((RealmIR.SamlRealmIR) realm);
                    isFrontendRealm = true;
                    break;
                case "pki":
                    newDomain = createPkiDomain((RealmIR.PkiRealmIR) realm);
                    realmNotImplementedReport(realmName, realm);

                    break;
                case "oidc":
                    newDomain = createOidcDomain((RealmIR.OidcRealmIR) realm);
                    isFrontendRealm = true;
                    break;
                case "kerberos":
                    newDomain = createKerberosDomain((RealmIR.KerberosRealmIR) realm);
                    isFrontendRealm = true;
                    break;
                default:
                    realmNotImplementedReport(realmName, realm);
                    break;
            }

            if (newDomain != null) {
                if (isFrontendRealm) {
                    fconfig.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(SG_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_frontend_authc.yml");
                } else {
                    config.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(SG_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_authc.yml");
                }
            }

        });

        return new Configs(config, fconfig);
    }

    private static void realmNotImplementedReport(String realmName, RealmIR realm) {
        MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, realmName, String.format("Realm migration for type %s not yet implemented.", realm.getType()));

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
        MigrationReport.shared.addMigrated(SG_AUTHC_FILE_NAME, key);
    }

    /**
     * Creates the LDAP-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createLdapDomain(RealmIR.LdapRealmIR ir) {
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

    /**
     * Creates the SAML-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createSAMLDomain(RealmIR.SamlRealmIR ir) {
        Map<String, Object> samlConfig = new HashMap<>();
        addOptionalConfigProperty(samlConfig, "user_mapping.roles.from", "change me");
        addOptionalConfigProperty(samlConfig, "saml.idp.metadata_file", ir.getIdpMetadataPath());
        addOptionalConfigProperty(samlConfig, "saml.sp.entity_id", ir.getSpEntityID());
        String spAcs = ir.getSpAcs();
        if (spAcs != null) {
            MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, "saml.sp.acs",
                    String.format(
                            "%s cannot be converted, domain is fixed to: https://<kibana-host>:<port>/searchguard/saml/acs",
                            spAcs
                    )
            );
        }

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                samlConfig,
                null
        );
    }

    /**
     * Creates the KEREBEROS-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createKerberosDomain(RealmIR.KerberosRealmIR ir) {
        Map<String, Object> kerberosConfig = new HashMap<>();
        addOptionalConfigProperty(kerberosConfig, "kerberos.krb_debug", ir.getKrbDebug());
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_keytab", ir.getPrincipal());
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_principal", ir.getKeytabPath());
        addOptionalConfigProperty(kerberosConfig, "kerberos.strip_realm_from_principal", ir.getRemoveRealmName());

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                kerberosConfig,
                null
        );
    }

    //TODO Implement these functions. They are just place holders for now
    private static MigrateConfig.NewAuthDomain createFileDomain(RealmIR.FileRealmIR ir) {
        return null;
    }
    private static MigrateConfig.NewAuthDomain createNativeDomain(RealmIR.NativeRealmIR ir) {
        return null;
    }

    private static MigrateConfig.NewAuthDomain createPkiDomain(RealmIR.PkiRealmIR ir) {

        return null;
    }


    /**
     * Creates the OIDC-Auth-Domain for sg_authc.yml
     * @param ir The IR that holds the config info
     * @return NewAuthDomain
     */
    private static MigrateConfig.NewAuthDomain createOidcDomain(RealmIR.OidcRealmIR ir) {
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
