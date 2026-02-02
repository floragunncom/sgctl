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
import java.util.*;

public class JwtTranslator extends RealmTranslator{
    @Override
    public NewAuthDomain translate(RealmIR originalIR) {
        //TODO: Not yet final
        RealmIR.JwtRealmIR ir = (RealmIR.JwtRealmIR) originalIR;
        //Todo: Use getter and remove addManualAction
        addOptionalConfigProperty("jwt.jwks_endpoint.url", ir.getPkcJwksetPath());

        String issuer = getFirstAndWarnIfMultiple("allowed_issuers", ir.getAllowedIssuersList());
        addOptionalConfigProperty("jwt.required_issuer", issuer);

        String audience = getFirstAndWarnIfMultiple("allowed_audiences", ir.getAllowedAudiences());
        addOptionalConfigProperty("jwt.required_audience", audience);

        String algorithm = getFirstAndWarnIfMultiple("allowed_algorithms", ir.getAllowedSignatureAlgorithms());
        addOptionalConfigProperty("jwt.allowed_algorithms", algorithm);

        String principalClaim = ir.getClaimsPrincipal() != null ? ir.getClaimsPrincipal() : "sub";
        addOptionalConfigProperty("user_mapping.user_name.from", principalClaim);

        String groupsClaim = ir.getClaimsGroups() != null ? ir.getClaimsGroups() : "roles";
        addOptionalConfigProperty("user_mapping.roles.from", groupsClaim);
        addOptionalConfigProperty("order", ir.getOrder());

        return new NewAuthDomain(
                "jwt",
                config
        );
    }

    private static String getFirstAndWarnIfMultiple(String fieldName, List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        if (list.size() > 1) {
            MigrationReport.shared.addManualAction(
                    "sg_authc.yml",
                    "jwt" + "." + fieldName,
                    "Multiple values found in Elasticsearch config. " +
                            "Only the first one ('" + list.get(0) + "') was migrated. Please check if this is correct."
            );
        }

        return list.get(0);
    }
}