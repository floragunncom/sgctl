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


package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.*;
import com.google.common.collect.ImmutableMap;

import java.util.*;

public class SGAuthcWriter {
    private final Map<String, RealmTranslator> realmMapping = new HashMap<>();
    private final SgAuthc config;
    private final SgAuthc frontEndConfig;
    public static final String SG_AUTHC_FILE_NAME = "sg_authc.yml";
    public static final String SG_FRONTEND_AUTHC_FILE_NAME = "sg_frontend_authc.yml";


    public SgAuthc getConfig() {
        return config;
    }

    public SgAuthc getFrontEndConfig() {
        return frontEndConfig;
    }

    public SGAuthcWriter(IntermediateRepresentationElasticSearchYml ir) {
        config = new SgAuthc(new ArrayList<>(), null, null);
        frontEndConfig = new SgAuthc(SG_FRONTEND_AUTHC_FILE_NAME, new ArrayList<>(), null, null);

        realmMapping.put("ldap", new LdapTranslator());
        realmMapping.put("file", new FileTranslator());
        realmMapping.put("native", new NativeTranslator());
        realmMapping.put("saml", new SAMLTranslator());
        realmMapping.put("pki", new PkiTranslator());
        realmMapping.put("oidc", new OidcTranslator());
        realmMapping.put("kerberos", new KerberosTranslator());
        realmMapping.put("jwt", new JwtTranslator());

        createAuthcConfig(ir);
    }

    /**
     * Creates Authc Config
     *
     * @param ir The intermediate representation.
     */
    private void createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {

        ir.getAuthent().getRealms().forEach((String realmName, RealmIR realm) -> {
            //Handle disabled realms, like discussed
            String type = realm.getType().toLowerCase().trim();

            if (!realm.isEnabled()) {
                MigrationReport.shared.addIgnoredKey(SG_AUTHC_FILE_NAME, type, "authdomains");
                return;
            }
            RealmTranslator translator = realmMapping.get(type);
            if (translator == null) {
                RealmTranslator.unknownRealmReport(realm);
                return;
            }
            RealmTranslator.NewAuthDomain newDomain = translator.translate(realm);

            if (newDomain != null) {
                if (translator.getIsFrontEnd()) {
                    frontEndConfig.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(SG_FRONTEND_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_frontend_authc.yml");
                } else {
                    config.authDomains.add(newDomain);
                    MigrationReport.shared.addMigrated(SG_AUTHC_FILE_NAME, realmName, "Realm migrated to sg_authc.yml");
                }
            }
        });
    }

    public static class SgAuthc implements Document<SgAuthc> {
        String fileName;

        List<RealmTranslator.NewAuthDomain> authDomains;
        String internalProxies;
        String remoteIpHeader;
        public SgAuthc(String fileName, List<RealmTranslator.NewAuthDomain> authDomains, String internalProxies, String remoteIpHeader) {
            this.fileName = fileName;
            this.authDomains = authDomains;
            this.internalProxies = internalProxies;
            this.remoteIpHeader = remoteIpHeader;
        }

        public SgAuthc(List<RealmTranslator.NewAuthDomain> authDomains, String internalProxies, String remoteIpHeader) {
            this.fileName = SG_AUTHC_FILE_NAME;
            this.authDomains = authDomains;
            this.internalProxies = internalProxies;
            this.remoteIpHeader = remoteIpHeader;
        }

        @Override
        public Object toBasicObject() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("auth_domains", authDomains);

            if (internalProxies != null || remoteIpHeader != null) {

                Map<String, Object> network = new LinkedHashMap<>();

                if (internalProxies != null) {
                    network.put("trusted_proxies_regex", internalProxies);
                }

                if (remoteIpHeader != null) {
                    network.put("http", ImmutableMap.of("remote_ip_header", remoteIpHeader));
                }

                result.put("network", network);
            }

            return result;
        }

        public Collection<RealmTranslator.NewAuthDomain> authDomains() {
            return this.authDomains;
        }
    }
}
