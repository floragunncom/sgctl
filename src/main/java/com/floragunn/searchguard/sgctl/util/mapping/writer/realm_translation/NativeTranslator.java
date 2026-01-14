package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class NativeTranslator extends RealmTranslator{
    @Override
    public NewAuthDomain translate(RealmIR ir) {
        return new NewAuthDomain(
                "basic",
                "internal_users_db",
                config,
                null
        );
    }
}
