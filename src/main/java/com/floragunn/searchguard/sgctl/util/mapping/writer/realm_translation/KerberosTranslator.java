package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class KerberosTranslator extends RealmTranslator {
    public KerberosTranslator() {
        isFrontEnd = true;
    }
    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.KerberosRealmIR ir = (RealmIR.KerberosRealmIR) originalIR;

        addOptionalConfigProperty("kerberos.krb_debug", ir.getKrbDebug());
        addOptionalConfigProperty("kerberos.acceptor_keytab", ir.getPrincipal());
        addOptionalConfigProperty("kerberos.acceptor_principal", ir.getKeytabPath());
        addOptionalConfigProperty("kerberos.strip_realm_from_principal", ir.getRemoveRealmName());

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                config,
                null
        );
    }
}
