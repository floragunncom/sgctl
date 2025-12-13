package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.HashMap;
import java.util.Map;

public class KerberosTranslator extends RealmTranslator {
    public KerberosTranslator() {
        isFrontEnd = true;
    }
    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.KerberosRealmIR ir = (RealmIR.KerberosRealmIR) originalIR;
        
        Map<String, Object> kerberosConfig = new HashMap<>();
        addOptionalConfigProperty(kerberosConfig, "kerberos.krb_debug", ir.getKrbDebug());
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_keytab", ir.getPrincipal());
        addOptionalConfigProperty(kerberosConfig, "kerberos.acceptor_principal", ir.getKeytabPath());
        addOptionalConfigProperty(kerberosConfig, "kerberos.strip_realm_from_principal", ir.getRemoveRealmName());

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                kerberosConfig,
                null
        );
    }
}
