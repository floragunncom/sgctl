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
     * @param sgAuthcFrontend true if in sg_frontend_authc section
     */
    private static void addOptionalConfigProperty(Map<String, Object> config, String key, Object value, Boolean sgAuthcFrontend) {
        if (value == null)
            return;
        config.put(key, value);

        String fileName;
        if(Boolean.TRUE.equals(sgAuthcFrontend)){
            fileName = SG_FRONTEND_AUTHC_FILE_NAME;
        }else{
            fileName = SG_AUTHC_FILE_NAME;
        }
        MigrationReport.shared.addMigrated(fileName, key);


    }

    private static String toBasicType(String type) {
        return String.format("basic/%s", type);
    }

    /**
     * Converts Xpack Attribute filter to Searchguard one. Defaults to "sub"
     *
     * @param xpackFilter the filter expression in xpack
     * @return converted filter
     */

    private static String convertXpackFilterToSearchguard(String xpackFilter) {
        // Handles conversion from X-Pack formats
        // sub_level → sub
        // one_level → one
        // subordinate_subtree → sub (fallback)
        // etc.
        switch (xpackFilter) {
            case "sub_level":
                return "sub";
            case "one_level":
                return "one";
            case "base":
                return "base_dn";
            default:
                MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, "ldap.user_search.filter.by_attribute", String.format("Unkown Attribute %s, defaulted to sub", xpackFilter));
                return "sub";
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
            addOptionalConfigProperty(ldapConfig, "ldap.idp.hosts", ldapHosts, false);
        }

        addOptionalConfigProperty(ldapConfig, "ldap.idp.bind_dn", ir.getBindDn(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.idp.password", ir.getBindPassword(), false);

        // User Search
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.base_dn", ir.getUserSearchBaseDn(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.raw", ir.getUserSearchFilter(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.scope", ir.getUserSearchScope(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.by_attribute", convertXpackFilterToSearchguard(ir.getUserSearchAttribute()), false);
        addOptionalConfigProperty(ldapConfig, "user_mapping.user_name.from", ir.getUserSearchUsernameAttribute(), false);

        // Group Search
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.base_dn", ir.getGroupSearchBaseDn(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.scope", ir.getGroupSearchScope(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.filter.raw", ir.getGroupSearchFilter(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.role_name_attribute", ir.getGroupSearchAttribute(), false);

        // Recursive groups
        Boolean unmappedGroups = ir.getUnmappedGroupsAsRoles();
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.recursive.enabled", unmappedGroups, false);

        // TLS/SSL
        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.verify_hostnames", ir.getSslVerificationMode(), false);

        List<String> cas = ir.getCertificateAuthorities();
        if (cas != null && !cas.isEmpty()) {
            addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.trusted_cas", cas, false);
        }

        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.client_auth.certificate", ir.getSslKeystorePath(), false);
        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.client_auth.private_key_password", ir.getSslKeystorePassword(), false);

        // Connection Pool
        addOptionalConfigProperty(ldapConfig, "ldap.idp.connection_strategy", ir.getLoadBalanceType(), false);

        // getTimeoutTcpConnect(), getTimeoutLdapRead(), getTimeoutLdapSearch() are not supported
        if (ir.getTimeoutTcpConnect() != null) {
            MigrationReport.shared.addWarning(SG_AUTHC_FILE_NAME, "Timeout Tcp Connect", "Timeout Tcp Connect is not supported in Searchguard");
        }
        if (ir.getTimeoutLdapSearch() != null) {
            MigrationReport.shared.addWarning(SG_AUTHC_FILE_NAME, "Timeout Tcp Search", "Timeout Tcp Search is not supported in Searchguard");
        }
        if (ir.getTimeoutLdapRead() != null) {
            MigrationReport.shared.addWarning(SG_AUTHC_FILE_NAME, "Timeout Tcp Read", "Timeout Tcp Read is not supported in Searchguard");
        }

        // Set default connection pool sizes if not already configured
        ldapConfig.putIfAbsent("ldap.idp.connection_pool.min_size", 3);
        ldapConfig.putIfAbsent("ldap.idp.connection_pool.max_size", 10);

        return new MigrateConfig.NewAuthDomain(
                toBasicType(ir.getType()),
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
                toBasicType(ir.getType()),
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
                toBasicType(ir.getType()),
                null,
                null,
                null,
                kerberosConfig,
                null
        );
    }

    //TODO Implement these functions. They are just place holders for now
    private static MigrateConfig.NewAuthDomain createFileDomain(RealmIR.FileRealmIR ir) {
        //TODO: This is technically a roles thing I think so it doesn't really work here (Needs to be discussed)

        return null;
    }
    private static MigrateConfig.NewAuthDomain createNativeDomain(RealmIR.NativeRealmIR ir) {
        return null;
    }

    private static String convertXpackUsernamePatternToSearchGuard(String usernamePattern, String usernameAttribute) {
        // If usernameAttribute is set, use it directly
        if (usernameAttribute != null && !usernameAttribute.isEmpty()) {
            return "clientcert.subject." + usernameAttribute.toLowerCase();
        }

        // If pattern is provided, try to extract the component
        if (usernamePattern != null && !usernamePattern.isEmpty()) {
            String pattern = usernamePattern.toUpperCase();

            // Common patterns
            if (pattern.contains("CN=")) {
                return "clientcert.subject.cn";
            } else if (pattern.contains("EMAILADDRESS=") || pattern.contains("EMAIL=")) {
                return "clientcert.subject.email_address";
            } else if (pattern.contains("O=")) {
                return "clientcert.subject.o";
            } else if (pattern.contains("OU=")) {
                return "clientcert.subject.ou";
            } else if (pattern.contains("C=")) {
                return "clientcert.subject.c";
            } else {
                // Pattern is too complex to convert automatically
                MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, "user_mapping.user_name.from", "Pattern is too complex to be converted, please add it manually");
                return null;
            }
        }

        // Default: use full DN
        return "clientcert.subject";
    }

    private static MigrateConfig.NewAuthDomain createPkiDomain(RealmIR.PkiRealmIR ir) {
        //TODO This has a few things that need to be added to the TLS Config in Elasticsearch.yml
        Map<String, Object> clientCertConfig = new HashMap<>();

        addOptionalConfigProperty(clientCertConfig, "user_mapping.user_name.from", convertXpackUsernamePatternToSearchGuard(ir.getUsernamePattern(), ir.getUsernameAttribute()), false);

        return new MigrateConfig.NewAuthDomain(
                "clientcert",
                null,
                null,
                null,
                clientCertConfig,
                null
        );
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
        //Sonar Cube was unhappy so I just added this rq
        String needsToBeAddedManually = "needs to be added manualy";
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.tls.trusted_cas", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "user_mapping.roles.from_comma_separated_string", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.proxy", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.client_secret", needsToBeAddedManually);


        return new MigrateConfig.NewAuthDomain(
                toBasicType(ir.getType()),
                null,
                null,
                null,
                oidcConfig,
                null
        );
    }
}
