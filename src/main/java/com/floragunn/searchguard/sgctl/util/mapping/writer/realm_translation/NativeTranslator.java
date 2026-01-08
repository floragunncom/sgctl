package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class NativeTranslator extends RealmTranslator{
    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR ir) {
        return new MigrateConfig.NewAuthDomain(
                "basic",
                "internal_users_db",
                null,
                null,
                config,
                null
        );
    }
}
