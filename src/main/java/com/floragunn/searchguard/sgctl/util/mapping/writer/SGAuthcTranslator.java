package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.*;

import java.util.*;

public class SGAuthcTranslator {
    private final Map<String, RealmTranslator> realmMapping = new HashMap<>();
    private final MigrateConfig.SgAuthc config;
    private final MigrateConfig.SgAuthc frontEndConfig;

    public MigrateConfig.SgAuthc getConfig() {
        return config;
    }

    public MigrateConfig.SgAuthc getFrontEndConfig() {
        return frontEndConfig;
    }

    public SGAuthcTranslator(IntermediateRepresentationElasticSearchYml ir) {
        config = new MigrateConfig.SgAuthc();
        frontEndConfig = new MigrateConfig.SgAuthc();

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
        config.authDomains = new ArrayList<>();
        frontEndConfig.authDomains = new ArrayList<>();
        frontEndConfig.internalProxies = "";
        frontEndConfig.remoteIpHeader = "";

        ir.getAuthent().getRealms().forEach((String realmName, RealmIR realm) -> {
            //Handle disabled realms, like discussed
            String type = realm.getType().toLowerCase().trim();

            //TODO: Is this a Debug-Flag?
            //System.out.println(realm.isEnabled());

            if (!realm.isEnabled()) {
                MigrationReport.shared.addIgnoredKey(RealmTranslator.SG_AUTHC_FILE_NAME, type, "authdomains");
                return;
            }
            RealmTranslator translator = realmMapping.get(type);
            if (translator == null) {
                RealmTranslator.realmNotImplementedReport(realmName, realm);
                return;
            }
            MigrateConfig.NewAuthDomain newDomain = translator.translate(realm);

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
}
