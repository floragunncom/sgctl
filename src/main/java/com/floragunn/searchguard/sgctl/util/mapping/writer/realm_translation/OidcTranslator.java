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

public class OidcTranslator extends RealmTranslator {
    public OidcTranslator() {
        isFrontEnd = true;
    }
    @Override
    public NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.OidcRealmIR ir = (RealmIR.OidcRealmIR) originalIR;

        //TODO review mapping: oidc.idp.openid_configuration_url, oidc.idp.tls.trusted_cas

        // 1. RP settings
        addOptionalConfigProperty("oidc.client_id", ir.getRpClientId());
        addOptionalConfigProperty("oidc.logout_url", ir.getRpPostLogoutRedirectUri());
        addOptionalConfigProperty("user_mapping.user_name.from.json_path", "oidc_id_token."+ ir.getClaimName());
        addOptionalConfigProperty("user_mapping.user_name.from.pattern", "oidc_id_token."+ ir.getClaimMail());
        addOptionalConfigProperty("oidc.idp.openid_configuration_url", ir.getOpIssuer()+ ".well-known/openid-configuration");
        //Sonar Cube was unhappy so I just added this rq
        String needsToBeAddedManually = "needs to be added manualy";
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.tls.trusted_cas", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "user_mapping.roles.from_comma_separated_string", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.idp.proxy", needsToBeAddedManually);
        MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "oidc.client_secret", needsToBeAddedManually);

        addOptionalConfigProperty("order", ir.getOrder());

        return new NewAuthDomain(
                ir.getType(),
                config
        );
    }
}
