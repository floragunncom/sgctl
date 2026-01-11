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

import java.net.InetSocketAddress;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.sgctl.testsupport.ExternalTestSupport;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;

public class MoveSearchGuardIndexCommandTest {

    private static LocalCluster cluster;
    private static String configDir;

    @BeforeAll
    public static void connect() throws Exception {
        ExternalTestSupport.assumeExternalTestsEnabled();
        cluster = new LocalCluster.Builder()
                .singleNode()
                .sslEnabled()
                .embedded()
                .start();

        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config").toString();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()),
                "--cert", adminCert, "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert,
                "--debug", "--sgctl-config-dir", configDir);

        Assertions.assertEquals(0, rc);
    }

    @AfterAll
    public static void destroy() throws Exception {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    public void shouldMoveSearchGuardIndex() throws Exception {
        int rc = SgctlTool.exec("move-sg-index", "--debug", "--sgctl-config-dir", configDir);
        Assertions.assertEquals(0, rc);
    }
}
