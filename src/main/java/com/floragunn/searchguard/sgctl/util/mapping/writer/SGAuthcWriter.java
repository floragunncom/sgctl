package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.*;
import com.google.common.collect.ImmutableMap;

import java.util.*;

public class SGAuthcWriter {
    private final Map<String, RealmTranslator> realmMapping = new HashMap<>();
    private final SgAuthc config;
    private final SgAuthc frontEndConfig;

    public SgAuthc getConfig() {
        return config;
    }

    public SgAuthc getFrontEndConfig() {
        return frontEndConfig;
    }

    public SGAuthcWriter(IntermediateRepresentationElasticSearchYml ir) {
        config = new SgAuthc(new ArrayList<>(), null, null);
        frontEndConfig = new SgAuthc(new ArrayList<>(), null, null);

        realmMapping.put("ldap", new LdapTranslator());
        realmMapping.put("file", new FileTranslator());
        realmMapping.put("native", new NativeTranslator());
        realmMapping.put("saml", new SAMLTranslator());
        realmMapping.put("pki", new PkiTranslator());
        realmMapping.put("oidc", new OidcTranslator());
        realmMapping.put("kerberos", new KerberosTranslator());
        realmMapping.put("jwt", new JwtTranslator());

        createAuthcConfig(ir);
    }

    /**
     * Creates Authc Config
     *
     * @param ir The intermediate representation.
     */
    private void createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {

        ir.getAuthent().getRealms().forEach((String realmName, RealmIR realm) -> {
            //Handle disabled realms, like discussed
            String type = realm.getType().toLowerCase().trim();

            if (!realm.isEnabled()) {
                MigrationReport.shared.addIgnoredKey(RealmTranslator.SG_AUTHC_FILE_NAME, type, "authdomains");
                return;
            }
            RealmTranslator translator = realmMapping.get(type);
            if (translator == null) {
                RealmTranslator.realmNotImplementedReport(realmName, realm);
                return;
            }
            RealmTranslator.NewAuthDomain newDomain = translator.translate(realm);

            if (newDomain != null) {
                if (translator.getIsFrontEnd()) {
                    frontEndConfig.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(RealmTranslator.SG_FRONTEND_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_frontend_authc.yml");
                } else {
                    config.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(RealmTranslator.SG_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_authc.yml");
                }
            }
        });
    }

    public record SgAuthc(List<RealmTranslator.NewAuthDomain> authDomains, String internalProxies, String remoteIpHeader) implements Document<SgAuthc> {

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
