package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RealmIR {
    String type; // ldap, saml, oidc, ...
    String name;
    int order;
    boolean enabled;

    public String getType() { return type; }
    public String getName() { return name; }
    public int getOrder() { return order; }
    public boolean isEnabled() { return enabled; }

    public RealmIR(String type, String name) {
        this.type = type;
        this.name = name;
    }

    String THIS_FILE = "elasticsearch.yml";
    // each realm type implements its own handler, attribute is suffix after xpack.security.authc.realms.<type>.<name>.
    public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
        return;
    }

    public static RealmIR create(String type, String name) {
        switch(type) {
            case "ldap": return new LdapRealmIR(name);
            case "file": return new FileRealmIR(name);
            case "native": return new NativeRealmIR(name);
            case "saml": return new SamlRealmIR(name);
            case "pki": return new PkiRealmIR(name);
            case "oidc": return new OidcRealmIR(name);
            case "kerberos": return new KerberosRealmIR(name);
            case "jwt": return new JwtRealmIR(name);
            default:
                return new UnknownRealmIR(type, name);
        }
    }

    // have the complete ldap config as this is the most used realm
    public static class LdapRealmIR extends RealmIR {
        // connection
        String url;
        String bindDn;
        String bindPassword;
        Boolean followReferrals;

        // user search
        String userSearchBaseDn;
        String userSearchFilter;
        String userSearchScope;
        String userSearchAttribute;
        String userSearchUsernameAttribute;

        // group search
        String groupSearchBaseDn;
        String groupSearchScope;
        String groupSearchFilter;
        String groupSearchAttribute;

        // Authorization
        String filesRolesMapping;
        Boolean unmappedGroupsAsRoles;

        // SSL
        List<String> certificateAuthorities = new ArrayList<>();
        String sslVerificationMode;
        String sslTruststorePath;
        String sslTruststorePassword;
        String sslTruststoreType;
        String sslKeystorePath;
        String sslKeystorePassword;
        String sslKeystoreType;

        // Load balancing
        String loadBalanceType;
        String loadBalanceCacheTtl;

        // Timeouts
        String timeoutTcpConnect;
        String timeoutLdapRead;
        String timeoutLdapSearch;

        public String getUrl(){return url;}
        public String getBindDn(){return bindDn;}
        public String getBindPassword(){return bindPassword;}
        public Boolean getFollowReferrals(){return followReferrals;}
        public String getUserSearchBaseDn(){return userSearchBaseDn;}
        public String getUserSearchFilter(){return userSearchFilter;}
        public String getUserSearchScope(){return userSearchScope;}
        public String getUserSearchAttribute(){return userSearchAttribute;}
        public String getUserSearchUsernameAttribute(){return userSearchUsernameAttribute;}
        public String getGroupSearchBaseDn(){return groupSearchBaseDn;}
        public String getGroupSearchScope(){return groupSearchScope;}
        public String getGroupSearchFilter(){return groupSearchFilter;}
        public String getGroupSearchAttribute(){return groupSearchAttribute;}
        public String getFilesRolesMapping(){return filesRolesMapping;}
        public Boolean getUnmappedGroupsAsRoles(){return unmappedGroupsAsRoles;}
        public List<String> getCertificateAuthorities(){return certificateAuthorities;}
        public String getSslVerificationMode(){return sslVerificationMode;}
        public String getSslTruststorePath(){return sslTruststorePath;}
        public String getSslTruststorePassword(){return sslTruststorePassword;}
        public String getSslTruststoreType(){return sslTruststoreType;}
        public String getSslKeystorePath(){return sslKeystorePath;}
        public String getSslKeystorePassword(){return sslKeystorePassword;}
        public String getSslKeystoreType(){return sslKeystoreType;}
        public String getLoadBalanceType(){return loadBalanceType;}
        public String getLoadBalanceCacheTtl(){return loadBalanceCacheTtl;}
        public String getTimeoutTcpConnect(){return timeoutTcpConnect;}
        public String getTimeoutLdapRead(){return timeoutLdapRead;}
        public String getTimeoutLdapSearch(){return timeoutLdapSearch;}



        LdapRealmIR(String name) {
            super("ldap", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    case "follow_referrals": this.followReferrals = (Boolean) value; break;
                    case "unmapped_groups_as_roles": this.unmappedGroupsAsRoles = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "url": this.url = (String) value; break;
                    case "bindDn": this.bindDn = (String) value; break;
                    case "bind_password": this.bindPassword = (String) value; break;

                    case "user_search.base_dn": this.userSearchBaseDn = (String) value; break;
                    case "user_search.filter": this.userSearchFilter = (String) value; break;
                    case "user_search.scope": this.userSearchScope = (String) value; break;
                    case "user_search.attribute": this.userSearchAttribute = (String) value; break;
                    case "user_search.username_attribute": this.userSearchUsernameAttribute = (String) value; break;

                    case "group_search.base_dn": this.groupSearchBaseDn = (String) value; break;
                    case "group_search.filter": this.groupSearchFilter = (String) value; break;
                    case "group_search.scope": this.groupSearchScope = (String) value; break;
                    case "group_search.attribute": this.groupSearchAttribute = (String) value; break;

                    case "files.role_mapping": this.filesRolesMapping = (String) value; break;

                    case "ssl.verification_mode": this.sslVerificationMode = (String) value; break;
                    case "ssl.truststore.path": this.sslTruststorePath = (String) value; break;
                    case "ssl.truststore.password": this.sslTruststorePassword = (String) value; break;
                    case "ssl.truststore.type": this.sslTruststoreType = (String) value; break;
                    case "ssl.keystore.path": this.sslKeystorePath = (String) value; break;
                    case "ssl.keystore.password": this.sslKeystorePassword = (String) value; break;
                    case "ssl.keystore.type": this.sslKeystoreType = (String) value; break;

                    case "load_balance.type": this.loadBalanceType = (String) value; break;
                    case "load_balance.cache.ttl": this.loadBalanceCacheTtl = (String) value; break;

                    case "timeout.tcp_connect": this.timeoutTcpConnect = (String) value; break;
                    case "timeout.ldap_read": this.timeoutLdapRead = (String) value; break;
                    case "timeout.ldap_search": this.timeoutLdapSearch = (String) value; break;

                    case "metadata":
                        MigrationReport.shared.addIgnoredKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
                        break;

                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, List.class)) {
                List<?> v = (List<?>) value;
                if (v.isEmpty())
                    return;
                if (v.get(0) instanceof String) {
                    switch (attribute) {
                        case "ssl.certificate_authorities": this.certificateAuthorities = (List<String>) value; break;
                        default: keyKnown = false; break;
                    }
                } else {
                    MigrationReport.shared.addManualAction(
                        THIS_FILE,
                        keyPrefix + attribute,
                        value + " is not a string but it should be"
                    );
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }

        @Override
        public String toString() {
            return ElasticsearchYamlReader.getFieldsAsString(this) + "order: " + this.order + "\nenabled: " + this.enabled;
        }
    }

    public static class FileRealmIR extends RealmIR {
        String filesUsers;
        String filesUsersRoles;

        public String getFilesUsersRoles() { return filesUsersRoles; }
        public String getFilesUsers() { return filesUsers; }

        FileRealmIR(String name) {
            super("file", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "files.users": this.filesUsers = (String) value;break;
                    case "files.users_roles": this.filesUsersRoles = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    public static class NativeRealmIR extends RealmIR {

        String cacheTtl;
        int cacheMaxUsers;

        public String getCacheTtl() { return cacheTtl; }
        public int getCacheMaxUsers() { return cacheMaxUsers; }

        NativeRealmIR(String name) {
            super("native", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "cache.ttl": this.cacheTtl = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    case "cache.max_users": this.cacheMaxUsers = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    public static class SamlRealmIR extends RealmIR {

        String idpMetadataPath;
        String spEntityID;
        String spAcs;
        String attributesPrincipal;

        public String getAttributesPrincipal() { return attributesPrincipal; }
        public String getIdpMetadataPath() { return idpMetadataPath; }
        public String getSpEntityID() { return spEntityID; }
        public String getSpAcs() { return spAcs; }

        SamlRealmIR(String name) {
            super("saml", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "idp.metadata.path": this.idpMetadataPath = (String) value; break;
                    case "sp.entity_id": this.spEntityID = (String) value; break;
                    case "sp.acs": this.spAcs = (String) value; break;
                    case "attributes.principal": this.attributesPrincipal = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    public static class PkiRealmIR extends RealmIR {

        List<String> certificateAuthorities = new ArrayList<>();

        String usernamePattern;
        String usernameAttribute;
        Boolean delegationEnabled;

        String truststorePath;
        String truststoreType;
        String truststorePassword;

        public List<String> getCertificateAuthorities() { return certificateAuthorities; }
        public String getUsernamePattern() { return usernamePattern; }
        public String getUsernameAttribute() { return usernameAttribute; }
        public Boolean getDelegationEnabled() { return delegationEnabled; }
        public String getTruststorePath() { return truststorePath; }
        public String getTruststoreType() { return truststoreType; }
        public String getTruststorePassword() { return truststorePassword; }

        PkiRealmIR(String name) {
            super("pki", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    case "delegation.enabled": this.delegationEnabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "username_pattern": this.usernamePattern = (String) value; break;
                    case "username_attribute": this.usernameAttribute = (String) value; break;
                    case "truststore.path": this.truststorePath = (String) value; break;
                    case "truststore.type": this.truststoreType = (String) value; break;
                    case "truststore.password": this.truststorePassword = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, List.class)) {
                List<?> v = (List<?>) value;

                if (v.isEmpty()) {
                    return;
                }

                if (!(v.get(0) instanceof String)) {
                    System.out.println("Bad list type in attribute " + attribute);
                }

                switch (attribute) {
                    case "certificate_authorities": this.certificateAuthorities = (List<String>) v; break;
                    default: keyKnown = false; break;
                }
            }

            else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    public static class OidcRealmIR extends RealmIR {

        // RP settings
        String rpClientId;
        String rpResponseType;
        String rpPostLogoutRedirectUri;

        // OP settings
        String opIssuer;
        String opAuthEndpoint;
        String opTokenEndpoint;
        String opJwkSetPath;

        // Claims
        String claimPrincipal;
        String claimName;
        String claimMail;
        String claimGroups;

        public String getRpClientId() { return rpClientId; }
        public String getRpResponseType() { return rpResponseType; }
        public String getRpPostLogoutRedirectUri() { return rpPostLogoutRedirectUri; }
        public String getOpIssuer() { return opIssuer; }
        public String getOpAuthEndpoint() { return opAuthEndpoint; }
        public String getOpTokenEndpoint() { return opTokenEndpoint; }
        public String getOpJwkSetPath() { return opJwkSetPath; }
        public String getClaimPrincipal() { return claimPrincipal; }
        public String getClaimName() { return claimName; }
        public String getClaimMail() { return claimMail; }
        public String getClaimGroups() { return claimGroups; }

        OidcRealmIR(String name) {
            super("oidc", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "rp.client_id": this.rpClientId = (String) value; break;
                    case "rp.response_type": this.rpResponseType = (String) value; break;
                    case "rp.post_logout_redirect_uri": this.rpPostLogoutRedirectUri = (String) value; break;
                    case "op.issuer": this.opIssuer = (String) value; break;
                    case "op.authorization_endpoint": this.opAuthEndpoint = (String) value; break;
                    case "op.token_endpoint": this.opTokenEndpoint = (String) value; break;
                    case "op.jwkset_path": this.opJwkSetPath = (String) value; break;
                    case "claims.principal": this.claimPrincipal = (String) value; break;
                    case "claims.name": this.claimName = (String) value; break;
                    case "claims.mail": this.claimMail = (String) value; break;
                    case "claims.groups": this.claimGroups = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    public static class KerberosRealmIR extends RealmIR {

        String keytabPath;
        String principal;
        Boolean krbDebug;
        Boolean removeRealmName;

        public String getKeytabPath() { return keytabPath; }
        public String getPrincipal() { return principal; }
        public Boolean getKrbDebug() { return krbDebug; }
        public Boolean getRemoveRealmName() { return removeRealmName; }

        KerberosRealmIR(String name) {
            super("kerberos", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    case "krb.debug": this.krbDebug = (Boolean) value; break;
                    case "remove_realm_name": this.removeRealmName = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "keytab.path": this.keytabPath = (String) value; break;
                    case "principal": this.principal = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }
    }

    static class JwtRealmIR extends RealmIR {

        String tokenType;
        String clientAuthenticationType;
        String allowedIssuers;
        List<String> allowedIssuersList;
        List<String> allowedAudiences;
        List<String> allowedSignatureAlgorithms;
        String pkcJwksetPath;
        String claimsPrincipal;
        String claimsGroups;

        public String getTokenType() { return tokenType; }
        public String getClientAuthenticationType() { return clientAuthenticationType; }
        public String getAllowedIssuers() { return allowedIssuers; }
        public List<String> getAllowedIssuersList() { return allowedIssuersList; }
        public List<String> getAllowedAudiences() { return allowedAudiences; }
        public List<String> getAllowedSignatureAlgorithms() { return allowedSignatureAlgorithms; }
        public String getPkcJwksetPath() { return pkcJwksetPath; }
        public String getClaimsPrincipal() { return claimsPrincipal; }
        public String getClaimsGroups() { return claimsGroups; }

        JwtRealmIR(String name) { super("jwt", name); }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.assertType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "token_type": this.tokenType = (String) value; break;
                    case "client_authentication.type": this.clientAuthenticationType = (String) value; break;
                    case "allowed_issuer": this.allowedIssuers = (String) value; break;
                    case "pkc_jwkset_path": this.pkcJwksetPath = (String) value; break;
                    case "claims.principal": this.claimsPrincipal = (String) value; break;
                    case "claims.groups": this.claimsGroups = (String) value; break;

                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.assertType(value, List.class)) {
                List<?> v = (List<?>) value;

                if (v.isEmpty()) {
                    return;
                }

                if (!(v.get(0) instanceof String)) {
                    System.out.println("Bad list type in attribute " + attribute);
                }

                switch (attribute) {
                    case "allowed_audiences": this.allowedAudiences = (List<String>) v; break;
                    case "allowed_signature_algorithms": this.allowedSignatureAlgorithms = (List<String>) v; break;
                    case "allowed_issuer": this.allowedIssuersList = (List<String>) v; break;

                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, keyPrefix + attribute);
            }
        }

    }

    static class UnknownRealmIR extends RealmIR {
        UnknownRealmIR(String type, String name) {
            super(type, name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unknown realm type " + type);
        }
    }
}
