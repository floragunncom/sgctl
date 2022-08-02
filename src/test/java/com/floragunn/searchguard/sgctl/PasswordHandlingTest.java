package com.floragunn.searchguard.sgctl;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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

    private final static TestCertificates TEST_CERTIFICATES = TestCertificates.builder().ca("CN=root.ca.example.com,OU=SearchGuard,O=SearchGuard")
            .addNodes("CN=127.0.0.1,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, null).build();

    private static LocalCluster cluster;

    static Path configDir;

    @BeforeAll
    public static void connect() throws Exception {
        cluster = new LocalCluster.Builder().singleNode().sslEnabled(TEST_CERTIFICATES).start();

        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config");

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                "--key", adminKey, "--key-pass", "", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir.toString(), "--save-key-pass", "false");

        Assertions.assertEquals(0, rc);
    }

    @AfterAll
    public static void destroy() throws Exception {
        cluster.close();
    }

    @Test
    public void test() throws Exception {
        Path sgConfigDestinationDir = Files.createTempDirectory("sgctl-test-sgconfig");
        int result = SgctlTool.exec("get-config", "-o", sgConfigDestinationDir.toString(), "--debug", "--sgctl-config-dir", configDir.toString());
        Assertions.assertEquals(0, result);

        System.out.println(Arrays.toString(configDir.toFile().list()));
        System.out.println(DocReader.yaml().readObject(new File(configDir + "/cluster_127.0.0.1.yml")));
    }
}
