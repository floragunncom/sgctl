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

package com.floragunn.searchguard.sgctl;

import static java.util.Collections.singletonList;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import com.google.common.base.Charsets;

public class ClusterInitializationTest {
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(new TeeOutputStream(outputStreamCaptor, standardOut)));
        System.setErr(new PrintStream(new TeeOutputStream(errStreamCaptor, standardErr)));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    private final static TestCertificates TEST_CERTIFICATES = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();

    public static LocalCluster cluster;

    static String configDir;

    @BeforeAll
    public static void connect() throws Exception {
        cluster = new LocalCluster.Builder()
                .singleNode()
                .sslEnabled(TEST_CERTIFICATES)
                .nodeSettings("entitlements.enabled", "false")
                .jvmArgs("-Djava.security.manager=")
                .useExternalProcessCluster()
                .start();

        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config").toString();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir,
                "--skip-connection-check");

        Assertions.assertEquals(0, rc);
    }

    @AfterAll
    public static void destroy() throws Exception {
        cluster.close();
    }

    @Test
    public void test() throws Exception {
        Path sgConfigDir = Files.createTempDirectory("sgctl-test-sgconfig");

        Files.write(sgConfigDir.resolve("sg_authc.yml"), ("auth_domains:\n- type: basic/internal_users_db").getBytes());
        Files.write(sgConfigDir.resolve("sg_internal_users.yml"),
                ("admin:\n  hash: \"$2a$12$VcCDgh2NDk07JGN0rjGbM.Ad41qVR/YFJcgHp0UGns5JDymv..TOG\"\n  reserved: true\n  backend_roles: [\"admin\"]")
                        .getBytes());
        Files.write(sgConfigDir.resolve("sg_roles.yml"), ("test_role:\n  cluster_permissions: ['SGS_CLUSTER_MONITOR']").getBytes());
        Files.write(sgConfigDir.resolve("sg_roles_mapping.yml"), ("role:\n  backend_roles: ['backend_role']").getBytes());
        Files.write(sgConfigDir.resolve("sg_tenants.yml"), ("admin_tenant:\n  description: \"Demo tenant\"").getBytes());
        Files.write(sgConfigDir.resolve("sg_action_groups.yml"), ("ag:\n  allowed_actions: [\"indices:data/read/search*\"]\n  type: \"index\"").getBytes());

        int rc = SgctlTool.exec("update-config", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir, "--skip-connection-check");

        Assertions.assertEquals(0, rc);

        Path sgConfigDir2 = Files.createTempDirectory("sgctl-test-sgconfig-updated");

        Thread.sleep(100);

        rc = SgctlTool.exec("get-config", "-o", sgConfigDir2.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assertions.assertEquals(0, rc);

        String sgRoles = com.google.common.io.Files.asCharSource(sgConfigDir2.resolve("sg_roles.yml").toFile(), Charsets.UTF_8).read();

        Assertions.assertTrue(sgRoles.contains("test_role"), sgRoles);
    }
}
