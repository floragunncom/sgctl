package com.floragunn.searchguard.sgctl;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.Format;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static LocalCluster cluster;
    private static Path configDir;
    private InetSocketAddress httpAddress;
    private String adminCert;
    private String adminKey;
    private String rootCaCert;

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PrivateKeyPassword {
        private final TestCertificates testCertificates = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
                .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
                .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();

        @BeforeAll
        public void setupCluster() {
            cluster = new LocalCluster.Builder().singleNode().sslEnabled(testCertificates).start();

            httpAddress = cluster.getHttpAddress();
            TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
            adminCert = adminCertificate.getCertificateFile().getPath();
            adminKey = adminCertificate.getPrivateKeyFile().getPath();
            rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        }

        @AfterAll
        public void destroy() {
            cluster.close();
        }

        @Test
        public void testSave() throws Exception {
            configDir = Files.createTempDirectory("sgctl-test-config");
            int result = SgctlTool.exec("connect", "--debug",
                    "-h", httpAddress.getHostString(),
                    "-p", String.valueOf(httpAddress.getPort()),
                    "--cert", adminCert,
                    "--key", adminKey,
                    "--ca-cert", rootCaCert,
                    "--sgctl-config-dir", configDir.toString(),
                    "--save-key-pass", "true",
                    "--key-pass", "secret");
            Assertions.assertEquals(0, result);

            Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
            Assertions.assertEquals(0, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
            Assertions.assertEquals(1, result);

            Assertions.assertTrue(configContainsKeyPass());
            Assertions.assertEquals("secret", getConfigKeyPass());
        }

        @Test
        public void testNoSave() throws Exception {
            configDir = Files.createTempDirectory("sgctl-test-config");
            int result = SgctlTool.exec("connect", "--debug",
                    "-h", httpAddress.getHostString(),
                    "-p", String.valueOf(httpAddress.getPort()),
                    "--cert", adminCert,
                    "--key", adminKey,
                    "--ca-cert", rootCaCert,
                    "--sgctl-config-dir", configDir.toString(),
                    "--save-key-pass", "false",
                    "--key-pass", "secret");
            Assertions.assertEquals(0, result);

            Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
            Assertions.assertEquals(1, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
            Assertions.assertEquals(1, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "secret");
            Assertions.assertEquals(0, result);

            Assertions.assertFalse(configContainsKeyPass());
            Assertions.assertNull(getConfigKeyPass());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NoPrivateKeyPassword {
        private final TestCertificates testCertificates = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
                .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
                .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, null).build();

        @BeforeAll
        public void setupCluster() {
            cluster = new LocalCluster.Builder().singleNode().sslEnabled(testCertificates).start();

            httpAddress = cluster.getHttpAddress();
            TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
            adminCert = adminCertificate.getCertificateFile().getPath();
            adminKey = adminCertificate.getPrivateKeyFile().getPath();
            rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        }

        @AfterAll
        public void destroy() {
            cluster.close();
        }

        @Test
        public void testSave() throws Exception {
            configDir = Files.createTempDirectory("sgctl-test-config");
            int result = SgctlTool.exec("connect", "--debug",
                    "-h", httpAddress.getHostString(),
                    "-p", String.valueOf(httpAddress.getPort()),
                    "--cert", adminCert,
                    "--key", adminKey,
                    "--ca-cert", rootCaCert,
                    "--sgctl-config-dir", configDir.toString(),
                    "--save-key-pass", "true");
            Assertions.assertEquals(0, result);

            Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
            Assertions.assertEquals(0, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
            Assertions.assertEquals(0, result);

            Assertions.assertTrue(configContainsKeyPass());
            Assertions.assertNull(getConfigKeyPass());
        }


        @Test
        public void testNoSave() throws Exception {
            configDir = Files.createTempDirectory("sgctl-test-config");
            int result = SgctlTool.exec("connect", "--debug",
                    "-h", httpAddress.getHostString(),
                    "-p", String.valueOf(httpAddress.getPort()),
                    "--cert", adminCert,
                    "--key", adminKey,
                    "--ca-cert", rootCaCert,
                    "--sgctl-config-dir", configDir.toString(),
                    "--save-key-pass", "false");
            Assertions.assertEquals(0, result);

            Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
            Assertions.assertEquals(0, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "s");
            Assertions.assertEquals(0, result);

            result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString(), "--key-pass", "secret");
            Assertions.assertEquals(0, result);

            Assertions.assertTrue(configContainsKeyPass());
            Assertions.assertNull(getConfigKeyPass());
        }
    }

    boolean configContainsKeyPass() throws Exception {
        File clusterConfigFile = new File(configDir.toFile(), "cluster_" + cluster.getHttpAddress().getHostString() + ".yml");

        Map<String, Object> config = DocReader.yaml().readObject(clusterConfigFile);
        Map<String, Object> clientAuthConfig = (Map<String, Object>) ((Map<String, Object>) config.get("tls")).get("client_auth");
        return clientAuthConfig.containsKey("private_key_password");
    }

    String getConfigKeyPass() throws Exception {
        File clusterConfigFile = new File(configDir.toFile(), "cluster_" + cluster.getHttpAddress().getHostString() + ".yml");
        return (String) DocNode.parse(Format.YAML).from(clusterConfigFile).get("tls", "client_auth", "private_key_password");
    }
}
