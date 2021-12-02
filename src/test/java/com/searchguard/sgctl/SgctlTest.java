/*
 * Copyright 2021 floragunn GmbH
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

package com.searchguard.sgctl;

import static java.util.Collections.singletonList;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.sgctl.util.YamlRewriter;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.RewriteResult;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

public class SgctlTest {

    private final static TestCertificates TEST_CERTIFICATES = TestCertificates.builder()
            .ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret")
            .build();

    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().singleNode().sslEnabled(TEST_CERTIFICATES).build();

    static String configDir;

    @BeforeClass
    public static void connect() throws Exception {
        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config").toString();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert",
                adminCert, "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug",
                "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);
    }

    @Test
    public void connectWithoutHOption() throws Exception {
        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        Path configDir = Files.createTempDirectory("sgctl-test-config");

        int rc = SgctlTool.exec("connect", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir",
                configDir.toString());

        Assert.assertEquals(0, rc);

        List<String> filesInConfigDir = Arrays.asList(configDir.toFile().list());

        Assert.assertTrue(filesInConfigDir.toString(), filesInConfigDir.contains("cluster_" + httpAddress.getHostString() + ".yml"));
    }

    @Test
    public void testDownloadAndUpload() throws Exception {

        Path sgConfigDir = Files.createTempDirectory("sgctl-test-sgconfig");

        int rc = SgctlTool.exec("get-config", "-o", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        File sgTenantsYml = new File(sgConfigDir.toFile(), "sg_tenants.yml");

        Assert.assertTrue(Arrays.asList(sgConfigDir.toFile().list()).toString(), sgTenantsYml.exists());

        YamlRewriter yamlRewriter = new YamlRewriter(sgTenantsYml);

        yamlRewriter
                .insertAtBeginning(new YamlRewriter.Attribute("sgctl_test_tenant", ImmutableMap.of("description", "Tenant added for testing sgctl")));

        RewriteResult rewriteResult = yamlRewriter.rewrite();

        com.google.common.io.Files.asCharSink(sgTenantsYml, Charsets.UTF_8).write(rewriteResult.getYaml());

        rc = SgctlTool.exec("update-config", sgTenantsYml.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        Path sgConfigDir2 = Files.createTempDirectory("sgctl-test-sgconfig-updated");

        Thread.sleep(100);

        rc = SgctlTool.exec("get-config", "-o", sgConfigDir2.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        Map<String, Object> sgTenants = DocReader.yaml().readObject(new File(sgConfigDir2.toFile(), "sg_tenants.yml"));

        Assert.assertTrue(sgTenants.toString(), sgTenants.containsKey("sgctl_test_tenant"));

    }

    @Test
    public void testCompleteDownloadAndUpload() throws Exception {

        Path sgConfigDir = Files.createTempDirectory("sgctl-test-sgconfig");

        int rc = SgctlTool.exec("get-config", "-o", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        File sgTenantsYml = new File(sgConfigDir.toFile(), "sg_tenants.yml");

        Assert.assertTrue(Arrays.asList(sgConfigDir.toFile().list()).toString(), sgTenantsYml.exists());

        YamlRewriter yamlRewriter = new YamlRewriter(sgTenantsYml);

        yamlRewriter.insertAtBeginning(
                new YamlRewriter.Attribute("sgctl_test_tenant2", ImmutableMap.of("description", "Tenant added for testing sgctl")));

        RewriteResult rewriteResult = yamlRewriter.rewrite();

        com.google.common.io.Files.asCharSink(sgTenantsYml, Charsets.UTF_8).write(rewriteResult.getYaml());

        rc = SgctlTool.exec("update-config", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        Path sgConfigDir2 = Files.createTempDirectory("sgctl-test-sgconfig-updated");

        Thread.sleep(100);

        rc = SgctlTool.exec("get-config", "-o", sgConfigDir2.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        Map<String, Object> sgTenants = DocReader.yaml().readObject(new File(sgConfigDir2.toFile(), "sg_tenants.yml"));

        Assert.assertTrue(sgTenants.toString(), sgTenants.containsKey("sgctl_test_tenant2"));

    }

    @Test
    public void testSgConfigValidation() throws Exception {

        Path sgConfigDir = Files.createTempDirectory("sgctl-test-sgconfig");

        int rc = SgctlTool.exec("get-config", "-o", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        File sgTenantsYml = new File(sgConfigDir.toFile(), "sg_tenants.yml");

        Assert.assertTrue(Arrays.asList(sgConfigDir.toFile().list()).toString(), sgTenantsYml.exists());

        YamlRewriter yamlRewriter = new YamlRewriter(sgTenantsYml);

        yamlRewriter.insertAtBeginning(
                new YamlRewriter.Attribute("sgctl_test_tenant_" + new Random().nextInt(), ImmutableMap.of("d", "Tenant added for testing sgctl")));

        RewriteResult rewriteResult = yamlRewriter.rewrite();

        com.google.common.io.Files.asCharSink(sgTenantsYml, Charsets.UTF_8).write(rewriteResult.getYaml());

        rc = SgctlTool.exec("update-config", sgTenantsYml.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(1, rc);

        // TODO check output

    }

    @Test
    public void testCommandLineValidation() throws Exception {
        String configDir = Files.createTempDirectory("sgctl-test-config").toString();
        InetSocketAddress httpAddress = cluster.getHttpAddress();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", "/nowhere",
                "--sgctl-config-dir", configDir);
        Assert.assertEquals(1, rc);

        // TODO check output

    }

    @Test
    public void testComponentState() throws Exception {

        int rc = SgctlTool.exec("component-state", "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        // TODO check output

    }

}
