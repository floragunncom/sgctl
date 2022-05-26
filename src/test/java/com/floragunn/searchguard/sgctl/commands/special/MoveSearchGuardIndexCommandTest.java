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

package com.floragunn.searchguard.sgctl.commands.special;

import static java.util.Collections.singletonList;

import java.net.InetSocketAddress;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.floragunn.searchguard.configuration.ConfigurationRepository;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.test.GenericRestClient;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;

public class MoveSearchGuardIndexCommandTest {

    private final static TestCertificates TEST_CERTIFICATES = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();

    @Test
    public void test() throws Exception {
        try (LocalCluster cluster = new LocalCluster.Builder().singleNode().sslEnabled(TEST_CERTIFICATES)//
                .configIndexName("searchguard")//
                .start()) {

            String configDir = Files.createTempDirectory("sgctl-test-config").toString();

            InetSocketAddress httpAddress = cluster.getHttpAddress();
            TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
            String adminCert = adminCertificate.getCertificateFile().getPath();
            String adminKey = adminCertificate.getPrivateKeyFile().getPath();
            String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();

            int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                    "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir);

            Assertions.assertEquals(0, rc);

            ConfigurationRepository configurationRespository = cluster.getInjectable(ConfigurationRepository.class);
            Assert.assertEquals("searchguard", configurationRespository.getEffectiveSearchGuardIndex());

            // Actual test:            
            rc = SgctlTool.exec("special", "move-sg-index", "--debug", "--sgctl-config-dir", configDir);

            Assertions.assertEquals(0, rc);

            try (GenericRestClient restClient = cluster.getAdminCertRestClient()) {
                Thread.sleep(100);
                Assert.assertEquals(".searchguard", configurationRespository.getEffectiveSearchGuardIndex());
            }
        }
    }
}
