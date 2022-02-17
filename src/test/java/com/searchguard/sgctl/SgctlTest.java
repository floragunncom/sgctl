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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Format;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.sgctl.util.YamlRewriter;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.RewriteResult;
import com.floragunn.searchguard.test.GenericRestClient;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import com.floragunn.searchsupport.util.ImmutableSet;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

public class SgctlTest {

    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @Before
    public void setUp() {
        System.setOut(new PrintStream(new TeeOutputStream(outputStreamCaptor, standardOut)));
        System.setErr(new PrintStream(new TeeOutputStream(errStreamCaptor, standardErr)));
    }

    @After
    public void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    private final static TestCertificates TEST_CERTIFICATES = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();

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

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir);

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

        int rc = SgctlTool.exec("connect", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert, "--key",
                adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir.toString());

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

    @Test
    public void addUserLocalToDir() throws Exception {
        File tempDir = Files.createTempDirectory("sgctl-test-add-user-local").toFile();

        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user-local", userName, "-r", "sg-role1,sg-role2,sg-role3", "--backend-roles", "backend-role1,backend-role2", "-a",
                "a=1,b.c.d=2,e=foo", "--password", "pass", "-o", tempDir.toString());

        Assert.assertEquals(0, result);
        DocNode writtenDocument = DocNode.parse(Format.YAML).from(new File(tempDir, "sg_internal_users.yml"));

        Assert.assertEquals(writtenDocument.toYamlString(), ImmutableSet.of("sg-role1", "sg-role2", "sg-role3"),
                ImmutableSet.of(writtenDocument.getAsNode(userName).getListOfStrings("search_guard_roles")));
    }

    @Test
    public void testUserAdd_shouldCreate() throws Exception {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-r", "sg-role1,sg-role2", "--backend-roles", "backend-role1,backend-role2", "-a",
                "a=1,b.c.d=2,e=foo", "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/internal_users/" + userName);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), Arrays.asList("sg-role1", "sg-role2"),
                    response.getBodyAsDocNode().get("data", "search_guard_roles"));
        }
    }

    @Test
    public void testUserDelete() {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-r", "sg-role1,sg-role2", "--backend-roles", "backend-role1,backend-role2", "-a",
                "a=1,b.c.d=2,e=foo", "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("delete-user", userName, "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);
        Assert.assertTrue(outputStreamCaptor.toString().contains("User " + userName + " has been deleted"));
    }

    @Test
    public void testUserDelete_userShouldBeDeleted() throws Exception {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-r", "sg-role1,sg-role2", "--backend-roles", "backend-role1,backend-role2", "-a",
                "a=1,b.c.d=2,e=foo", "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("delete-user", userName, "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/internal_users/" + userName);

            Assert.assertEquals(response.getBody(), 404, response.getStatusCode());
        }
    }

    @Test
    public void testUserUpdate() {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-r", "sg-role1,sg-role2", "--backend-roles", "backend-role1,backend-role2", "-a",
                "a=1,b.c.d=2,e=foo", "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "-r", "sg-role1,sg-role3", "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);
        Assert.assertTrue(outputStreamCaptor.toString().contains("User " + userName + " has been updated"));
    }

    @Test
    public void testUserUpdate_passwordUpdate() {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "--password", "new_password", "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);
    }

    @Test
    public void testUserUpdate_searchGuardRolesUpdate() {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-r", "sg-role1,sg-role2", "--sgctl-config-dir", configDir, "--password", "pass",
                "--debug");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "-r", "new-sg-role1,new-sg-role2", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);
    }

    @Test
    public void testUserUpdate_backendRolesUpdate() throws Exception {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "--backend-roles", "backend-role1,backend-role2", "--sgctl-config-dir", configDir,
                "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "--backend-roles", "new-backend-role1,new-backend-role2", "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/internal_users/" + userName);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), Arrays.asList("backend-role1", "backend-role2", "new-backend-role1", "new-backend-role2"),
                    response.getBodyAsDocNode().get("data", "backend_roles"));
        }
    }

    @Test
    public void testUserUpdate_addAttributes() throws Exception {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-a", "a=1,b.c.d=2,e=foo", "--sgctl-config-dir", configDir, "--password", "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "-a", "a=new-1,b.c.d2=new-2,e.a=newEA", "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/internal_users/" + userName);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), DocNode.of("a", "new-1", "b.c.d2", "new-2", "b.c.d", "2", "e.a", "newEA").toNormalizedMap(),
                    response.getBodyAsDocNode().get("data", "attributes"));
        }
    }

    @Test
    public void testUserUpdate_removeAttributesUpdate() throws Exception {
        String userName = "userName_" + UUID.randomUUID();
        int result = SgctlTool.exec("add-user", userName, "-a", "a=1,b.c.d=2,e=foo,z.a=3,z.b=4", "--sgctl-config-dir", configDir, "--password",
                "pass");
        Assert.assertEquals(0, result);

        result = SgctlTool.exec("update-user", userName, "--remove-attributes", "a,b,z.a", "--sgctl-config-dir", configDir);
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/internal_users/" + userName);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), DocNode.of("e", "foo", "z.b", "4").toNormalizedMap(),
                    response.getBodyAsDocNode().get("data", "attributes"));
        }
    }

    @Test
    public void testAddAndDeleteConfigVar() throws Exception {
        String id = "add_test";
        String value = "foo";
        int result = SgctlTool.exec("add-var", id, value, "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/vars/" + id);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), value, response.getBodyAsDocNode().get("data", "value"));
        }

        result = SgctlTool.exec("delete-var", id, "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/vars/" + id);

            Assert.assertEquals(response.getBody(), 404, response.getStatusCode());
        }
    }

    @Test
    public void testAddConfigVarEncrypted() throws Exception {
        String id = "add_encrypted_test";
        String value = "foo";
        int result = SgctlTool.exec("add-var", id, value, "-e", "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/vars/" + id);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertNotNull(response.getBody(), response.getBodyAsDocNode().get("data", "encrypted", "value"));
        }
    }

    @Test
    public void testUpdateConfigVar() throws Exception {
        String id = "update_test";
        String value = "foo";
        int result = SgctlTool.exec("add-var", id, value, "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        value += "2";

        result = SgctlTool.exec("add-var", id, value, "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(1, result);

        result = SgctlTool.exec("update-var", id, value, "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/vars/" + id);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), value, response.getBodyAsDocNode().get("data", "value"));
        }
    }

    @Test
    public void testAddConfigVarFile() throws Exception {
        String id = "add_test_file";
        File file = File.createTempFile(id, ".json");
        DocNode value = DocNode.of("a", 1, "b", 2, "c", Arrays.asList("x", "y"));
        DocWriter.json().write(file, value);
        int result = SgctlTool.exec("add-var", id, "-i", file.getAbsolutePath(), "--scope", "scope", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/vars/" + id);

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), value.toMap(), response.getBodyAsDocNode().get("data", "value"));
        }
    }

    @Test
    public void setCommand() throws Exception {
        int result = SgctlTool.exec("set", "authc", "debug", "--true", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(0, result);

        try (GenericRestClient client = cluster.getAdminCertRestClient()) {
            GenericRestClient.HttpResponse response = client.get("/_searchguard/config/authc");

            Assert.assertEquals(response.getBody(), 200, response.getStatusCode());
            Assert.assertEquals(response.getBody(), Boolean.TRUE, response.getBodyAsDocNode().get("content", "debug"));
        }
    }

    @Test
    public void setCommand_invalidProperty() {
        int result = SgctlTool.exec("set", "authc", "debux", "--true", "--sgctl-config-dir", configDir, "--debug");
        Assert.assertEquals(1, result);
    }
}
