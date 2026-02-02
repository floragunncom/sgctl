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

public class PkiTranslator extends RealmTranslator {

    private static String convertXpackUsernamePatternToSearchGuard(String usernamePattern, String usernameAttribute) {
        // If usernameAttribute is set, use it directly
        if (usernameAttribute != null && !usernameAttribute.isEmpty()) {
            return "clientcert.subject." + usernameAttribute.toLowerCase();
        }

        // If pattern is provided, try to extract the component
        if (usernamePattern != null && !usernamePattern.isEmpty()) {
            String pattern = usernamePattern.toUpperCase();

            // Common patterns
            if (pattern.contains("CN=")) {
                return "clientcert.subject.cn";
            } else if (pattern.contains("EMAILADDRESS=") || pattern.contains("EMAIL=")) {
                return "clientcert.subject.email_address";
            } else if (pattern.contains("O=")) {
                return "clientcert.subject.o";
            } else if (pattern.contains("OU=")) {
                return "clientcert.subject.ou";
            } else if (pattern.contains("C=")) {
                return "clientcert.subject.c";
            } else {
                // Pattern is too complex to convert automatically
                MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, "user_mapping.user_name.from", "Pattern is too complex to be converted, please add it manually");
                return null;
            }
        }

        // Default: use full DN
        return "clientcert.subject";
    }

    @Override
    public NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.PkiRealmIR ir = (RealmIR.PkiRealmIR) originalIR;
        //TODO This has a few things that need to be added to the TLS Config in Elasticsearch.yml

        addOptionalConfigProperty("user_mapping.user_name.from", convertXpackUsernamePatternToSearchGuard(ir.getUsernamePattern(), ir.getUsernameAttribute()));
        addOptionalConfigProperty("order", ir.getOrder());

        return new NewAuthDomain(
                "clientcert",
                config
        );
    }
}
