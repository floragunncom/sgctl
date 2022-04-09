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
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.BackendUpdateInstructions;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.ConfigMigrator;
import com.floragunn.searchguard.test.GenericRestClient;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;

public class MigrateConfigTest {

    static LocalCluster cluster;

    @BeforeAll
    public static void connect() throws Exception {
        cluster = new LocalCluster.Builder().singleNode().sslEnabled().enterpriseModulesEnabled().start();
    }

    @AfterAll
    public static void destroy() throws Exception {
        cluster.close();
    }

    static Stream<Arguments> listConfigDirs() throws URISyntaxException {
        URL configDirUrl = MigrateConfigTest.class.getClassLoader().getResource("migrate_config");

        if (configDirUrl == null) {
            throw new RuntimeException("Could not find migrate_config resource directory");
        }

        File configDir = new File(configDirUrl.toURI());

        return Arrays.asList(configDir.listFiles()).stream().map((f) -> Arguments.of(f));
    }

    @ParameterizedTest
    @MethodSource("listConfigDirs")
    public void smokeTest(File configDir) throws Exception {
        File sgConfig = new File(configDir, "sg_config.yml");
        File kibanaYml = new File(configDir, "kibana.yml");

        MigrateConfig command = new MigrateConfig();
        ConfigMigrator configMigrator = command.new ConfigMigrator(sgConfig, kibanaYml.exists() ? kibanaYml : null, true, "kibana.yml");
        BackendUpdateInstructions backendUpdateInstructions = configMigrator.createBackendUpdateInstructions();

        System.out.println("Migrated sg_authc:\n" + backendUpdateInstructions.sgAuthc.toYamlString());

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.putJson("/_searchguard/config",
                    DocNode.of("authc.content", backendUpdateInstructions.sgAuthc.toBasicObject()));
            
            Assertions.assertEquals(200, response.getStatusCode(), response.getBody());
        }

    }

}
