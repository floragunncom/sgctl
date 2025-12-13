package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LdapTranslator extends RealmTranslator {

    /**
     * Converts Xpack Attribute filter to Searchguard one. Defaults to "sub"
     *
     * @param xpackFilter the filter expression in xpack
     * @return converted filter
     */

    private String convertXpackFilterToSearchguard(String xpackFilter) {
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

    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.LdapRealmIR ir = (RealmIR.LdapRealmIR) originalIR;
        Map<String, Object> ldapConfig = new HashMap<>();
        String url = ir.getUrl();
        if (url != null) {
            List<String> ldapHosts = Arrays.asList(url);
            addOptionalConfigProperty(ldapConfig, "ldap.idp.hosts", ldapHosts);
        }

        addOptionalConfigProperty(ldapConfig, "ldap.idp.bind_dn", ir.getBindDn());
        addOptionalConfigProperty(ldapConfig, "ldap.idp.password", ir.getBindPassword());

        // User Search
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.base_dn", ir.getUserSearchBaseDn());
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.raw", ir.getUserSearchFilter());
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.scope", ir.getUserSearchScope());
        addOptionalConfigProperty(ldapConfig, "ldap.user_search.filter.by_attribute", convertXpackFilterToSearchguard(ir.getUserSearchAttribute()));
        addOptionalConfigProperty(ldapConfig, "user_mapping.user_name.from", ir.getUserSearchUsernameAttribute());

        // Group Search
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.base_dn", ir.getGroupSearchBaseDn());
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.scope", ir.getGroupSearchScope());
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.filter.raw", ir.getGroupSearchFilter());
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.role_name_attribute", ir.getGroupSearchAttribute());

        // Recursive groups
        Boolean unmappedGroups = ir.getUnmappedGroupsAsRoles();
        addOptionalConfigProperty(ldapConfig, "ldap.group_search.recursive.enabled", unmappedGroups);

        // TLS/SSL
        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.verify_hostnames", ir.getSslVerificationMode());

        List<String> cas = ir.getCertificateAuthorities();
        if (cas != null && !cas.isEmpty()) {
            addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.trusted_cas", cas);
        }

        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.client_auth.certificate", ir.getSslKeystorePath());
        addOptionalConfigProperty(ldapConfig, "ldap.idp.tls.client_auth.private_key_password", ir.getSslKeystorePassword());

        // Connection Pool
        addOptionalConfigProperty(ldapConfig, "ldap.idp.connection_strategy", ir.getLoadBalanceType());

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
}
