package com.searchguard.sgctl;

import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;

public class UpdateSgLicenseCommandTest {
    private final static TestCertificates testCertificates = TestCertificates.builder()
                .ca("CN=localhost,OU=SearchGuard,O=SearchGuard")
                .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();
    private final static TestCertificate wmCert = testCertificates.create("CN=localhost,OU=SearchGuard,O=SearchGuard");
    @RegisterExtension
    private final static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig()
            .httpsPort(9200)
            .keystorePath(wmCert.getJksFile().getPath())
            .keystorePassword(wmCert.getPrivateKeyPassword())
            .keyManagerPassword(wmCert.getPrivateKeyPassword())
    ).build();

    private static String configDir;

    @BeforeAll
    public static void connect() throws Exception {
        configDir = Files.createTempDirectory("sgctl-test-config").toString();
        int result = SgctlTool.exec("connect", "-h", "localhost", "-p", String.valueOf(wm.getHttpsPort()),
                "--cert", testCertificates.getAdminCertificate().getCertificateFile().getPath(),
                "--key", testCertificates.getAdminCertificate().getPrivateKeyFile().getPath(),
                "--key-pass", "secret",
                "--ca-cert", testCertificates.getCaCertificate().getCertificateFile().getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");

        Assertions.assertEquals(0, result);
    }

    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(new TeeOutputStream(outputStreamCaptor, standardOut)));
        System.setErr(new PrintStream(new TeeOutputStream(errStreamCaptor, standardErr)));
        wm.stubFor(WireMock.put(urlEqualTo("/_searchguard/license/key"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 400,\"error\": {\"message\":\"error-message\"}}")));
        wm.stubFor(WireMock.put(urlEqualTo("/_searchguard/license/key"))
                .withRequestBody(equalToJson("{\"key\":\"valid\"}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"Configuration has been updated\"}")));
        wm.stubFor(WireMock.get(urlEqualTo("/_searchguard/license"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cluster_name\": \"searchguard_demo\",\"sg_license\":{\"uid\":\"00000000-0000-0000-0000-000000000000\",\"type\":\"TRIAL\",\"features\": [\"COMPLIANCE\"],\"issue_date\": \"2022-02-20\",\"expiry_date\": \"2022-04-21\"}}")));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    @Test
    public void testUpdateValidLicense() throws Exception {
        File validLicense = File.createTempFile("license", "txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(validLicense));
        writer.write("valid");
        writer.close();
        int result = SgctlTool.exec("update-license", "-l", validLicense.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(1, putRequestedFor(urlEqualTo("/_searchguard/license/key")));
        wm.verify(1, getRequestedFor(urlEqualTo("/_searchguard/license")));
    }

    @Test
    public void testUpdateInvalidLicense() throws Exception {
        File invalidLicense = File.createTempFile("license", "txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(invalidLicense));
        writer.write("invalid");
        writer.close();
        int result = SgctlTool.exec("update-license", "-l", invalidLicense.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);
        wm.verify(exactly(1), putRequestedFor(urlEqualTo("/_searchguard/license/key")));

        invalidLicense = File.createTempFile("empty", "txt");
        result = SgctlTool.exec("update-license", "-l", invalidLicense.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);
        wm.verify(exactly(2), putRequestedFor(urlEqualTo("/_searchguard/license/key")));

        result = SgctlTool.exec("update-license", "-l", "invalid-path",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);
        wm.verify(exactly(2), putRequestedFor(urlEqualTo("/_searchguard/license/key")));
        wm.verify(exactly(0), getRequestedFor(urlEqualTo("/_searchguard/license")));
    }
}
