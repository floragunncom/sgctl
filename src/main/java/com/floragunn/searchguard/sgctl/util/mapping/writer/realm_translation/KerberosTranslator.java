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
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class KerberosTranslator extends RealmTranslator {
    public KerberosTranslator() {
        isFrontEnd = true;
    }
    @Override
    public NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.KerberosRealmIR ir = (RealmIR.KerberosRealmIR) originalIR;

        addOptionalConfigProperty("kerberos.krb_debug", ir.getKrbDebug());
        addOptionalConfigProperty("kerberos.acceptor_keytab", ir.getPrincipal());
        addOptionalConfigProperty("kerberos.acceptor_principal", ir.getKeytabPath());
        addOptionalConfigProperty("kerberos.strip_realm_from_principal", ir.getRemoveRealmName());

        return new NewAuthDomain(
                ir.getType(),
                config
        );
    }
}
