package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.*;

public class SGAuthcTranslator {
    private static final String SG_AUTHC_FILE_NAME = "sg_authc.yml";
    private static final String SG_FRONTEND_AUTHC_FILE_NAME = "sg_frontend_authc.yml";

    public record Configs(MigrateConfig.SgAuthc config, MigrateConfig.SgAuthc fconfig) {

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
     * @param sg_frontend_authc true if in sg_frontend_authc section
     */
    private static void addOptionalConfigProperty(Map<String, Object> config, String key, Object value, Boolean sg_frontend_authc) {
        if (value == null)
            return;
        if(sg_frontend_authc){
            config.put(key, value);
            MigrationReport.shared.addMigrated(SG_FRONTEND_AUTHC_FILE_NAME, key);
        }else{
            config.put(key, value);
            MigrationReport.shared.addMigrated(SG_AUTHC_FILE_NAME, key);
        }

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
        addOptionalConfigProperty(ldapConfig, "ldap.idp.bind_dn", ir.getBindDn(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.base_dn", ir.getUserSearchBaseDn(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.raw", ir.getUserSearchFilter(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.base_dn", ir.getGroupSearchBaseDn(), false);

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
        addOptionalConfigProperty(samlConfig, "user_mapping.roles.from", "change me", true);
        addOptionalConfigProperty(samlConfig, "saml.idp.metadata_file", ir.getIdpMetadataPath(),true);
        addOptionalConfigProperty(samlConfig, "saml.sp.entity_id", ir.getSpEntityID(),true);
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
        addOptionalConfigProperty(kerberosConfig, "kerberos.krb_debug", ir.getKrbDebug(), true);
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_keytab", ir.getPrincipal(), true);
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_principal", ir.getKeytabPath(), true);
        addOptionalConfigProperty(kerberosConfig, "kerberos.strip_realm_from_principal", ir.getRemoveRealmName(), true);

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
        //TODO review mapping: oidc.idp.openid_configuration_url, oidc.idp.tls.trusted_cas

        // 1. RP settings
        addOptionalConfigProperty(oidcConfig, "oidc.client_id", ir.getRpClientId(), true);
        addOptionalConfigProperty(oidcConfig, "oidc.logout_url", ir.getRpPostLogoutRedirectUri(), true);
        addOptionalConfigProperty(oidcConfig, "user_mapping.user_name.from.json_path", "oidc_id_token."+ ir.getClaimName(), true);
        addOptionalConfigProperty(oidcConfig, "user_mapping.user_name.from.pattern", "oidc_id_token."+ ir.getClaimMail(), true);
        addOptionalConfigProperty(oidcConfig, "oidc.idp.openid_configuration_url", ir.getOpIssuer()+ ".well-known/openid-configuration", true);

        MigrationReport.shared.addManualAction("sg_frontend_authc.yml", "oidc.idp.tls.trusted_cas", "needs to be added manualy");
        MigrationReport.shared.addManualAction("sg_frontend_authc.yml", "user_mapping.roles.from_comma_separated_string", "needs to be added manualy");
        MigrationReport.shared.addManualAction("sg_frontend_authc.yml", "oidc.idp.proxy", "needs to be added manualy");
        MigrationReport.shared.addManualAction("sg_frontend_authc.yml", "oidc.client_secret", "needs to be added manualy");


        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                oidcConfig,
                null
        );
    }
}
