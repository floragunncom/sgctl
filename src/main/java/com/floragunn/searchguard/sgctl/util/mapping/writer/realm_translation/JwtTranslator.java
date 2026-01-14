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