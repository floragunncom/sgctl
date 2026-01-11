package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Intermediate representation of an authentication realm from elasticsearch.yml.
 */
public class RealmIR {
    protected String type; // ldap, saml, oidc, ...
    protected String name;
    protected int order;
    protected boolean enabled;

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
            default:
                //System.out.println("Unknown realm type: " + type);
                return new UnknownRealmIR(type, name);
        }
    }

    public static class LdapRealmIR extends RealmIR {
        private String url;
        private String bindDn;
        private String userSearchBaseDn;
        private String userSearchFilter;
        private String groupSearchBaseDn;

        public String getUrl() { return url; }
        public String getBindDn() { return bindDn; }
        public String getUserSearchBaseDn() { return userSearchBaseDn; }
        public String getUserSearchFilter() { return userSearchFilter; }
        public String getGroupSearchBaseDn() { return groupSearchBaseDn; }

        LdapRealmIR(String name) {
            super("ldap", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "url": this.url = (String) value; break;
                    case "bindDn": this.bindDn = (String) value; break;
                    case "user_search.base_dn": this.userSearchBaseDn = (String) value; break;
                    case "user_search.filter": this.userSearchFilter = (String) value; break;
                    case "group_search.base_dn": this.groupSearchBaseDn = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }

        @Override
        public String toString() {
            return ElasticsearchYamlReader.getFieldsAsString(this) + "order: " + this.order + "\nenabled: " + this.enabled;
        }
    }

    public static class FileRealmIR extends RealmIR {
        private String filesUsers;
        private String filesUsersRoles;

        public String getFilesUsersRoles() { return filesUsersRoles; }
        public String getFilesUsers() { return filesUsers; }

        FileRealmIR(String name) {
            super("file", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "files.users": this.filesUsers = (String) value;break;
                    case "files.users_roles": this.filesUsersRoles = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }
    }

    public static class NativeRealmIR extends RealmIR {

        private String cacheTtl;
        private int cacheMaxUsers;

        public String getCacheTtl() { return cacheTtl; }
        public int getCacheMaxUsers() { return cacheMaxUsers; }

        NativeRealmIR(String name) {
            super("native", name);
        }

        @Override
        public void handleAttribute(String attribute, Object value, String keyPrefix, File configFile) {
            boolean keyKnown = true;

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "cache.ttl": this.cacheTtl = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    case "cache.max_users": this.cacheMaxUsers = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }
    }

    public static class SamlRealmIR extends RealmIR {

        private String idpMetadataPath;
        private String spEntityID;
        private String spAcs;
        private String attributesPrincipal;

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

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "idp.metadata.path": this.idpMetadataPath = (String) value; break;
                    case "sp.entity_id": this.spEntityID = (String) value; break;
                    case "sp.acs": this.spAcs = (String) value; break;
                    case "attributes.principal": this.attributesPrincipal = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }
    }

    public static class PkiRealmIR extends RealmIR {

        private List<String> certificateAuthorities = new ArrayList<>();

        private String usernamePattern;
        private String usernameAttribute;
        private Boolean delegationEnabled;

        private String truststorePath;
        private String truststoreType;
        private String truststorePassword;

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

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    case "delegation.enabled": this.delegationEnabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "username_pattern": this.usernamePattern = (String) value; break;
                    case "username_attribute": this.usernameAttribute = (String) value; break;
                    case "truststore.path": this.truststorePath = (String) value; break;
                    case "truststore.type": this.truststoreType = (String) value; break;
                    case "truststore.password": this.truststorePassword = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, List.class)) {
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
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }
    }

    public static class OidcRealmIR extends RealmIR {

        // RP settings
        private String rpClientId;
        private String rpResponseType;
        private String rpPostLogoutRedirectUri;

        // OP settings
        private String opIssuer;
        private String opAuthEndpoint;
        private String opTokenEndpoint;
        private String opJwkSetPath;

        // Claims
        private String claimPrincipal;
        private String claimName;
        private String claimMail;
        private String claimGroups;

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

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
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
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            }
        }
    }

    public static class KerberosRealmIR extends RealmIR {

        private String keytabPath;
        private String principal;
        private Boolean krbDebug;
        private Boolean removeRealmName;

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

            if (IntermediateRepresentationElasticSearchYml.isType(value, Boolean.class)) {
                switch (attribute) {
                    case "enabled": this.enabled = (Boolean) value; break;
                    case "krb.debug": this.krbDebug = (Boolean) value; break;
                    case "remove_realm_name": this.removeRealmName = (Boolean) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, String.class)) {
                switch (attribute) {
                    case "type": this.type = (String) value; break;
                    case "keytab.path": this.keytabPath = (String) value; break;
                    case "principal": this.principal = (String) value; break;
                    default: keyKnown = false; break;
                }
            } else if (IntermediateRepresentationElasticSearchYml.isType(value, Integer.class)) {
                switch (attribute) {
                    case "order": this.order = (Integer) value; break;
                    default: keyKnown = false; break;
                }
            } else {
                MigrationReport.shared.addManualAction(THIS_FILE, keyPrefix + attribute, "Unexpected type " + value.getClass().getSimpleName());
            }

            if (!keyKnown) {
                MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + attribute, configFile.getPath());
            } else {
                MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + attribute, configFile.getPath());
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
