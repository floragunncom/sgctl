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
        public SgFrontEndAuthc fconfig;

        public Configs(MigrateConfig.SgAuthc config, SgFrontEndAuthc fconfig){
            this.config = config;
            this.fconfig = fconfig;
        }

    }

    /**
     * Creates Authc Config.
     * Führt Realms von X-Pack IR in die SG-Konfiguration (sg_authc.yml und sg_frontend_authc.yml) über.
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

        Map<String, RealmIR> realms = ir.getAuthent().getRealms();

        realms.forEach((String realmName, RealmIR realm) -> {
            String type = realm.getType();
            String keyPrefix = "xpack.security.authc.realms." + type + "." + realmName;
            MigrateConfig.NewAuthDomain newDomain = null;
            boolean isFrontendRealm = false;

            switch (type) {
                case "ldap":
                    newDomain = null;
                    isFrontendRealm = false;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "LDAP realm migration not yet implemented.");
                    break;
                case "file":
                    newDomain = null;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "File realm migration not yet implemented.");
                    break;
                case "native":
                    newDomain = null;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "Native realm migration not yet implemented.");
                    break;
                case "saml":
                    newDomain = null;
                    isFrontendRealm = true;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "SAML realm migration not yet implemented.");
                    break;
                case "pki":
                    newDomain = null;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "PKI realm migration not yet implemented.");
                    break;
                case "oidc":
                    newDomain = createOidcDomain(realmName, (RealmIR.OidcRealmIR) realm);
                    isFrontendRealm = true;
                    break;
                case "kerberos":
                    newDomain = null;
                    MigrationReport.shared.addManualAction("elasticsearch.yml", keyPrefix, "Kerberos realm migration not yet implemented.");
                    break;
                default:
                    return;
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
     * Creates the OIDC-Auth-Domain for sg_frontend_authc.yml
     */
    private static MigrateConfig.NewAuthDomain createOidcDomain(String realmName, RealmIR.OidcRealmIR ir) {
        Map<String, Object> oidcConfig = new HashMap<>();
        oidcConfig.put("test", "123");

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
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

            Map<String, Object> network = new LinkedHashMap<>();

            if (internalProxies != null || remoteIpHeader != null) {

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