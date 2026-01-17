/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.Arrays;
import java.util.List;

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
        if (xpackFilter == null || xpackFilter.isEmpty()) {
            return null;
        }
        return switch (xpackFilter) {
            case "sub_level" -> "sub";
            case "one_level" -> "one";
            case "base" -> "base_dn";
            default -> {
                MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, "ldap.user_search.filter.by_attribute", String.format("Unkown Attribute %s, defaulted to sub", xpackFilter));
                yield "sub";
            }
        };
    }

    @Override
    public NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.LdapRealmIR ir = (RealmIR.LdapRealmIR) originalIR;
        String url = ir.getUrl();
        if (url != null) {
            List<String> ldapHosts = Arrays.asList(url);
            addOptionalConfigProperty("ldap.idp.hosts", ldapHosts);
        }

        addOptionalConfigProperty("ldap.idp.bind_dn", ir.getBindDn());
        addOptionalConfigProperty("ldap.idp.password", ir.getBindPassword());

        // User Search
        addOptionalConfigProperty("ldap.user_search.base_dn", ir.getUserSearchBaseDn());
        addOptionalConfigProperty("ldap.user_search.filter.raw", ir.getUserSearchFilter());
        addOptionalConfigProperty("ldap.user_search.scope", ir.getUserSearchScope());
        addOptionalConfigProperty("ldap.user_search.filter.by_attribute", convertXpackFilterToSearchguard(ir.getUserSearchAttribute()));
        addOptionalConfigProperty("user_mapping.user_name.from", ir.getUserSearchUsernameAttribute());

        // Group Search
        addOptionalConfigProperty("ldap.group_search.base_dn", ir.getGroupSearchBaseDn());
        addOptionalConfigProperty("ldap.group_search.scope", ir.getGroupSearchScope());
        addOptionalConfigProperty("ldap.group_search.filter.raw", ir.getGroupSearchFilter());
        addOptionalConfigProperty("ldap.group_search.role_name_attribute", ir.getGroupSearchAttribute());

        // Recursive groups
        Boolean unmappedGroups = ir.getUnmappedGroupsAsRoles();
        addOptionalConfigProperty("ldap.group_search.recursive.enabled", unmappedGroups);

        // TLS/SSL
        addOptionalConfigProperty("ldap.idp.tls.verify_hostnames", ir.getSslVerificationMode());

        List<String> cas = ir.getCertificateAuthorities();
        if (cas != null && !cas.isEmpty()) {
            addOptionalConfigProperty("ldap.idp.tls.trusted_cas", cas);
        }

        addOptionalConfigProperty("ldap.idp.tls.client_auth.certificate", ir.getSslKeystorePath());
        addOptionalConfigProperty("ldap.idp.tls.client_auth.private_key_password", ir.getSslKeystorePassword());

        // Connection Pool
        addOptionalConfigProperty("ldap.idp.connection_strategy", ir.getLoadBalanceType());

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
        addOptionalConfigProperty("order", ir.getOrder());
        return new NewAuthDomain(
                toBasicType(ir.getType()),
                config
        );
    }
}
