package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.HashMap;
import java.util.Map;

public class OidcTranslator extends RealmTranslator {
    public OidcTranslator() {
        isFrontEnd = true;
    }
    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.OidcRealmIR ir = (RealmIR.OidcRealmIR) originalIR;

        Map<String, Object> oidcConfig = new HashMap<>();
        //TODO review mapping: oidc.idp.openid_configuration_url, oidc.idp.tls.trusted_cas

        // 1. RP settings
        addOptionalConfigProperty(oidcConfig, "oidc.client_id", ir.getRpClientId());
        addOptionalConfigProperty(oidcConfig, "oidc.logout_url", ir.getRpPostLogoutRedirectUri());
        addOptionalConfigProperty(oidcConfig, "user_mapping.user_name.from.json_path", "oidc_id_token."+ ir.getClaimName());
        addOptionalConfigProperty(oidcConfig, "user_mapping.user_name.from.pattern", "oidc_id_token."+ ir.getClaimMail());
        addOptionalConfigProperty(oidcConfig, "oidc.idp.openid_configuration_url", ir.getOpIssuer()+ ".well-known/openid-configuration");
        //Sonar Cube was unhappy so I just added this rq
        String needsToBeAddedManually = "needs to be added manualy";
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.tls.trusted_cas", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "user_mapping.roles.from_comma_separated_string", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.proxy", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.client_secret", needsToBeAddedManually);


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
