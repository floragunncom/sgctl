package com.floragunn.searchguard.sgctl;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class PasswordHandlingTest {
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

    private final static TestCertificates TEST_CERTIFICATES_PRIVATE_KEY_PASS = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();

    private final static TestCertificates TEST_CERTIFICATES_NO_PRIVAT_KEY_PASS = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, null).build();

    private static LocalCluster cluster;
    private static Path configDir;

    void prepareCluster(boolean keyPass, boolean saveKeyPass) throws IOException {
        cluster = new LocalCluster.Builder().singleNode().sslEnabled(keyPass ? TEST_CERTIFICATES_PRIVATE_KEY_PASS : TEST_CERTIFICATES_NO_PRIVAT_KEY_PASS).start();

        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config");

        List<String> args = keyPass ? ImmutableList.of(
                "connect", "--debug",
                "-h", httpAddress.getHostString(),
                "-p", String.valueOf(httpAddress.getPort()),
                "--cert", adminCert,
                "--key", adminKey,
                "--ca-cert", rootCaCert,
                "--sgctl-config-dir", configDir.toString(),
                "--save-key-pass", Boolean.toString(saveKeyPass),
                "--key-pass", "secret"
        ) : ImmutableList.of(
                "connect", "--debug",
                "-h", httpAddress.getHostString(),
                "-p", String.valueOf(httpAddress.getPort()),
                "--cert", adminCert,
                "--key", adminKey,
                "--ca-cert", rootCaCert,
                "--sgctl-config-dir", configDir.toString(),
                "--save-key-pass", Boolean.toString(saveKeyPass)
        );
        int rc = SgctlTool.exec(args.toArray(new String[0]));
        Assertions.assertEquals(0, rc);
    }

    @AfterEach
    public void destroy() throws Exception {
        cluster.close();
    }

    @Test
    public void testSaveKeyPass() throws Exception {
        prepareCluster(true, true);

        Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
        int result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
        Assertions.assertEquals(0, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
        Assertions.assertEquals(1, result);

        Assertions.assertTrue(configContainsKeyPass());
        Assertions.assertEquals("secret", getConfigKeyPass());
    }

    @Test
    public void testSaveNoKeyPass() throws Exception {
        prepareCluster(false, true);

        Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
        int result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
        Assertions.assertEquals(0, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
        Assertions.assertEquals(0, result);

        Assertions.assertTrue(configContainsKeyPass());
        Assertions.assertNull(getConfigKeyPass());
    }

    @Test
    public void testNoSaveKeyPass() throws Exception {
        prepareCluster(true, false);

        Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
        int result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
        Assertions.assertEquals(1, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
        Assertions.assertEquals(1, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "secret");
        Assertions.assertEquals(0, result);

        Assertions.assertFalse(configContainsKeyPass());
        Assertions.assertNull(getConfigKeyPass());
    }

    @Test
    public void testNoSaveNoKeyPass() throws Exception {
        prepareCluster(false, false);

        Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
        int result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
        Assertions.assertEquals(0, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
        Assertions.assertEquals(0, result);

        result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "secret");
        Assertions.assertEquals(0, result);

        Assertions.assertTrue(configContainsKeyPass());
        Assertions.assertNull(getConfigKeyPass());
    }

    boolean configContainsKeyPass() throws Exception {
        File clusterConfigFile = new File(configDir.toFile(), "cluster_" + cluster.getHttpAddress().getHostString() + ".yml");

        Map<String, Object> config = DocReader.yaml().readObject(clusterConfigFile);
        Map<String, Object> clientAuthConfig = (Map<String, Object>) ((Map<String, Object>) config.get("tls")).get("client_auth");
        return clientAuthConfig.containsKey("private_key_password");
    }

    String getConfigKeyPass() throws Exception {
        File clusterConfigFile = new File(configDir.toFile(), "cluster_" + cluster.getHttpAddress().getHostString() + ".yml");

        Map<String, Object> config = DocReader.yaml().readObject(clusterConfigFile);
        Map<String, Object> clientAuthConfig = (Map<String, Object>) ((Map<String, Object>) config.get("tls")).get("client_auth");
        return (String) clientAuthConfig.get("private_key_password");
    }
}
