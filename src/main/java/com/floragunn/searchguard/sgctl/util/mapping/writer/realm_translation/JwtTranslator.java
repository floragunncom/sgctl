package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class JwtTranslator extends RealmTranslator{
    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR ir) {
        //Todo: Use getter and remove addManualAction
        addOptionalConfigProperty("jwt.jwks_endpoint.url", "https://EXAMPLE/jwt/jwkset.json");
        MigrationReport.shared.addManualAction("sg_authc.yml", "jwt.jwks_endpoint.url", "Not yet implemented");

        addOptionalConfigProperty("jwt.required_issuer", "https://EXAMPLE/jwt/jwkset.json");
        MigrationReport.shared.addManualAction("sg_authc.yml", "jwt.required_issuer", "Not yet implemented");

        addOptionalConfigProperty("jwt.required_audience", "https://EXAMPLE/jwt/jwkset.json");
        MigrationReport.shared.addManualAction("sg_authc.yml", "jwt.required_audience", "Not yet implemented");

        addOptionalConfigProperty("user_mapping.user_name.from", "https://EXAMPLE/jwt/jwkset.json");
        MigrationReport.shared.addManualAction("sg_authc.yml", "user_mapping.user_name.from", "Not yet implemented");

        return new MigrateConfig.NewAuthDomain(
                "jwt",
                null,
                null,
                null,
                config,
                null
        );
    }
}