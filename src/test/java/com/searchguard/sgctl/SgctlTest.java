package com.searchguard.sgctl;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.sgctl.util.YamlRewriter;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.RewriteResult;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import com.floragunn.searchguard.test.helper.file.FileHelper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

public class SgctlTest {
    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().singleNode().sslEnabled().build();

    static String configDir;

    @BeforeClass
    public static void connect() throws Exception {
        InetSocketAddress httpAddress = cluster.getHttpAddress();
        Path adminCert = FileHelper.getAbsoluteFilePathFromClassPath("kirk-cert.pem");
        Path adminKey = FileHelper.getAbsoluteFilePathFromClassPath("kirk-key.pem");
        Path rootCaCert = FileHelper.getAbsoluteFilePathFromClassPath("root-ca.pem");
        configDir = Files.createTempDirectory("sgctl-test-config").toString();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert",
                adminCert.toString(), "--key", adminKey.toString(), "--key-pass", "secret", "--ca-cert", rootCaCert.toString(), "--debug",
                "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);
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
    public void testValidation() throws Exception {

        Path sgConfigDir = Files.createTempDirectory("sgctl-test-sgconfig");

        int rc = SgctlTool.exec("get-config", "-o", sgConfigDir.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(0, rc);

        File sgTenantsYml = new File(sgConfigDir.toFile(), "sg_tenants.yml");

        Assert.assertTrue(Arrays.asList(sgConfigDir.toFile().list()).toString(), sgTenantsYml.exists());

        YamlRewriter yamlRewriter = new YamlRewriter(sgTenantsYml);

        yamlRewriter.insertAtBeginning(new YamlRewriter.Attribute("sgctl_test_tenant", ImmutableMap.of("d", "Tenant added for testing sgctl")));

        RewriteResult rewriteResult = yamlRewriter.rewrite();

        com.google.common.io.Files.asCharSink(sgTenantsYml, Charsets.UTF_8).write(rewriteResult.getYaml());

        rc = SgctlTool.exec("update-config", sgTenantsYml.toString(), "--debug", "--sgctl-config-dir", configDir);

        Assert.assertEquals(1, rc);

        // TODO check output

    }
}
