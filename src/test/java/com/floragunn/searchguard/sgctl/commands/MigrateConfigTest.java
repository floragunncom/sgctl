/*
 * Copyright 2022 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.BackendUpdateInstructions;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.ConfigMigrator;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.FrontendUpdateInstructions;
import com.floragunn.searchguard.sgctl.testsupport.ExternalTestSupport;
import com.floragunn.searchguard.test.GenericRestClient;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;

public class MigrateConfigTest {

    static LocalCluster cluster;

    @BeforeAll
    public static void connect() throws Exception {
        ExternalTestSupport.assumeExternalTestsEnabled();
        cluster = new LocalCluster.Builder()
                .singleNode()
                .sslEnabled()
                .embedded()
                .start();
    }

    @AfterAll
    public static void destroy() throws Exception {
        if (cluster != null) {
            cluster.close();
        }
    }

    static Stream<Arguments> listConfigDirs() throws URISyntaxException {
        URL configDirUrl = MigrateConfigTest.class.getClassLoader().getResource("migrate_config");

        if (configDirUrl == null) {
            throw new RuntimeException("Could not find migrate_config resource directory");
        }

        File configDir = new File(configDirUrl.toURI());

        return Arrays.asList(configDir.listFiles()).stream().filter((f) -> new File(f, "sg_config.yml").exists()).map((f) -> Arguments.of(f));
    }

    @ParameterizedTest
    @MethodSource("listConfigDirs")
    public void smokeTest(File configDir) throws Exception {
        File sgConfig = new File(configDir, "sg_config.yml");
        File kibanaYml = new File(configDir, "kibana.yml");

        MigrateConfig command = new MigrateConfig();
        ConfigMigrator configMigrator = command.new ConfigMigrator(sgConfig, kibanaYml.exists() ? kibanaYml : null, true, "kibana.yml");
        BackendUpdateInstructions backendUpdateInstructions = configMigrator.createBackendUpdateInstructions();
        FrontendUpdateInstructions frontendUpdateInstructions = configMigrator.createUpdateInstructions();

        System.out.println("Migrated sg_authc:\n" + backendUpdateInstructions.sgAuthc.toYamlString());
        System.out.println(
                "Migrated sg_frontend_authc:\n" + (frontendUpdateInstructions != null && frontendUpdateInstructions.getSgFrontendConfig() != null
                        ? DocWriter.yaml().writeAsString(frontendUpdateInstructions.getSgFrontendConfig())
                        : "null"));
        System.out.println("Migrated sg_frontend_multitenancy:\n"
                + (backendUpdateInstructions.sgFrontendMultiTenancy != null ? backendUpdateInstructions.sgFrontendMultiTenancy.toYamlString()
                        : "null"));

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            Map<String, Object> updateBody = new LinkedHashMap<>();
            Map<String, Object> authcConfig = toPlainMap(backendUpdateInstructions.sgAuthc.toBasicObject());
            filterAuthcConfig(authcConfig);
            List<?> authDomains = authcConfig.get("auth_domains") instanceof List<?> list ? list : List.of();
            Assumptions.assumeTrue(!authDomains.isEmpty(),
                    "No supported authc domains for " + configDir.getName());
            updateBody.put("authc", ImmutableMap.of("content", authcConfig));

            if (frontendUpdateInstructions != null && frontendUpdateInstructions.getSgFrontendConfig() != null) {
                Map<String, Object> frontendConfig = filterFrontendAuthcConfig(frontendUpdateInstructions.getSgFrontendConfig());
                if (frontendConfig != null) {
                    updateBody.put("frontend_authc", ImmutableMap.of("content", frontendConfig));
                }
            }

            // frontend_multi_tenancy is not available in embedded test clusters

            GenericRestClient.HttpResponse response = client.putJson("/_searchguard/config", DocWriter.json().writeAsString(updateBody));

            Assertions.assertEquals(200, response.getStatusCode(), response.getBody());
        }

    }

    private Map<String, Object> toPlainMap(Object input) throws Exception {
        return DocReader.json().readObject(DocWriter.json().writeAsString(input));
    }

    @SuppressWarnings("unchecked")
    private void filterAuthcConfig(Map<String, Object> authcConfig) {
        Object authDomainsObj = authcConfig.get("auth_domains");
        if (!(authDomainsObj instanceof List<?> authDomains)) {
            return;
        }

        List<Object> filtered = new ArrayList<>();
        for (Object domainObj : authDomains) {
            if (!(domainObj instanceof Map<?, ?> domainMapRaw)) {
                continue;
            }
            Map<String, Object> domainMap = (Map<String, Object>) domainMapRaw;
            Object typeObj = domainMap.get("type");
            if (!(typeObj instanceof String type) || !isSupportedAuthcType(type)) {
                continue;
            }
            filterAdditionalUserInformation(domainMap);
            filtered.add(domainMap);
        }
        authcConfig.put("auth_domains", filtered);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterFrontendAuthcConfig(Map<String, Object> frontendConfig) throws Exception {
        Map<String, Object> copy = toPlainMap(frontendConfig);
        Object defaultObj = copy.get("default");
        if (!(defaultObj instanceof Map<?, ?> defaultMapRaw)) {
            return copy;
        }
        Map<String, Object> defaultMap = (Map<String, Object>) defaultMapRaw;
        Object authDomainsObj = defaultMap.get("auth_domains");
        if (!(authDomainsObj instanceof List<?> authDomains)) {
            return copy;
        }

        List<Object> filtered = new ArrayList<>();
        for (Object domainObj : authDomains) {
            if (!(domainObj instanceof Map<?, ?> domainMapRaw)) {
                continue;
            }
            Map<String, Object> domainMap = (Map<String, Object>) domainMapRaw;
            Object typeObj = domainMap.get("type");
            if (typeObj instanceof String type && isSupportedFrontendAuthcType(type)) {
                filtered.add(domainMap);
            }
        }

        if (filtered.isEmpty()) {
            return null;
        }

        defaultMap.put("auth_domains", filtered);
        copy.put("default", defaultMap);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private void filterAdditionalUserInformation(Map<String, Object> domainMap) {
        Object infoObj = domainMap.get("additional_user_information");
        if (!(infoObj instanceof List<?> infoList)) {
            return;
        }

        List<Object> filtered = new ArrayList<>();
        for (Object infoEntry : infoList) {
            if (!(infoEntry instanceof Map<?, ?> infoMapRaw)) {
                continue;
            }
            Map<String, Object> infoMap = (Map<String, Object>) infoMapRaw;
            Object typeObj = infoMap.get("type");
            if (typeObj instanceof String type && "internal_users_db".equals(type)) {
                filtered.add(infoMap);
            }
        }

        if (filtered.isEmpty()) {
            domainMap.remove("additional_user_information");
        } else {
            domainMap.put("additional_user_information", filtered);
        }
    }

    private boolean isSupportedAuthcType(String type) {
        return "basic/internal_users_db".equals(type)
                || "basic/noop".equals(type)
                || "anonymous".equals(type)
                || "trusted_origin".equals(type)
                || "clientcert".equals(type);
    }

    private boolean isSupportedFrontendAuthcType(String type) {
        return "basic".equals(type) || "link".equals(type);
    }
}
