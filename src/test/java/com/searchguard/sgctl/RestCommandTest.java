package com.searchguard.sgctl;

import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;

public class RestCommandTest {
    private final static TestCertificates testCertificates = TestCertificates.builder()
            .ca("CN=localhost,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();
    private final static TestCertificate wmCert = testCertificates.create("CN=localhost,OU=SearchGuard,O=SearchGuard");

    @RegisterExtension
    private final static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig()
            .httpDisabled(true)
            .dynamicHttpsPort()
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
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    @Test
    public void testInvalidInput() throws Exception {
        int result = SgctlTool.exec("rest", "put", "/some/endpoint", "--json", "{\"key\":\"value}",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);

        String json = "{\n" +
                "    \"value1\":\"Hello World\",\n" +
                "    \"value2\":3,\n" +
                "    \"obj\":{\n" +
                "        \"part1\":\"some value\",\n" +
                "        \"part2\":\n" +
                "            \"entry1\",\"entry2\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        File jsonFile = File.createTempFile("content", ".json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write(json);
        writer.close();
        result = SgctlTool.exec("rest", "put", "/some/endpoint", "--input", jsonFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);

        String content = "{\"content\"}";
        File contentFile = File.createTempFile("content", ".some");
        writer = new BufferedWriter(new FileWriter(contentFile));
        writer.write(content);
        writer.close();
        result = SgctlTool.exec("rest", "put", "/some/endpoint", "--input", contentFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);
    }

    @Test
    public void testOutputFile() throws Exception {
        Path outputPath = Files.createTempDirectory("outputs");
        wm.stubFor(get(urlEqualTo("/some/endpoint"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"ok\"}")));

        int result = SgctlTool.exec("rest", "get", "/some/endpoint", "--output", outputPath.toFile().getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);

        String fileName = Arrays.stream(Objects.requireNonNull(outputPath.toFile().list())).filter(s -> s.startsWith("response")).findFirst().get();
        BufferedReader reader = new BufferedReader(new FileReader(new File(outputPath.toFile().getPath(), fileName)));
        StringBuilder outputString = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) outputString.append(line).append("\n");
        reader.close();
        Assertions.assertEquals("{\n" +
                "  \"status\" : 200,\n" +
                "  \"message\" : \"ok\"\n" +
                "}",
                outputString.substring(0, outputString.lastIndexOf("\n")));

        File outputFile = File.createTempFile("out", "");
        result = SgctlTool.exec("rest", "get", "/some/endpoint", "--output", outputFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);

        reader = new BufferedReader(new FileReader(outputFile.getPath() + ".json"));
        outputString = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) outputString.append(line).append("\n");
        reader.close();
        Assertions.assertEquals("{\n" +
                        "  \"status\" : 200,\n" +
                        "  \"message\" : \"ok\"\n" +
                        "}",
                outputString.substring(0, outputString.lastIndexOf("\n")));

        outputFile = File.createTempFile("out", ".txt");
        result = SgctlTool.exec("rest", "get", "/some/endpoint", "--output", outputFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);

        reader = new BufferedReader(new FileReader(outputFile.getPath()));
        outputString = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) outputString.append(line).append("\n");
        reader.close();
        Assertions.assertEquals("{\n" +
                        "  \"status\" : 200,\n" +
                        "  \"message\" : \"ok\"\n" +
                        "}",
                outputString.substring(0, outputString.lastIndexOf("\n")));
    }

    @Test
    public void testGet() throws Exception {
        wm.stubFor(get(urlEqualTo("/some/endpoint"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"ok\"}")));

        int result = SgctlTool.exec("rest", "get", "/some/endpoint", "-i", "some_invalid_path", "--json", "{}",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testPutJson() throws Exception {
        String json = "{\"key\": \"value\", \"array\": [\"val1\", 2, 3], \"obj\": {}}";
        wm.stubFor(put(urlEqualTo("/some/endpoint"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(json))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"Configuration has been updated\"}")));

        int result = SgctlTool.exec("rest", "put", "/some/endpoint", "-i", "some_invalid_path", "--json", json,
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(1, result);

        result = SgctlTool.exec("rest", "put", "/some/endpoint", "--json", json,
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testPutJsonFile() throws Exception {
        String json = "{\n" +
                "    \"value1\":\"Hello World\",\n" +
                "    \"value2\":3,\n" +
                "    \"obj\":{\n" +
                "        \"part1\":\"some value\",\n" +
                "        \"part2\":[\n" +
                "            \"entry1\",\"entry2\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        File jsonFile = File.createTempFile("content", ".json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write(json);
        writer.close();
        wm.stubFor(put(urlEqualTo("/some/endpoint"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(json))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"Configuration has been updated\"}")));
        int result = SgctlTool.exec("rest", "put", "/some/endpoint", "-i", jsonFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(1), putRequestedFor(urlEqualTo("/some/endpoint")));
    }

    @Test
    public void testPutYamlFile() throws Exception {
        String yaml = "---\n" +
                "value1: \"Hello World\"\n" +
                "value2: 3\n" +
                "obj:\n" +
                "  part1: \"some value\"\n" +
                "  part2:\n" +
                "  - \"entry1\"\n" +
                "  - \"entry2\"";

        File ymlFile = File.createTempFile("content", ".yml");
        BufferedWriter writer = new BufferedWriter(new FileWriter(ymlFile));
        writer.write(yaml);
        writer.close();
        wm.stubFor(put(urlEqualTo("/some/endpoint"))
                .withHeader("Content-Type", equalTo("application/x-yaml"))
                .withRequestBody(equalTo(yaml))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200,\"message\": \"Configuration has been updated\"}")));

        int result = SgctlTool.exec("rest", "put", "/some/endpoint", "-i", ymlFile.getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(1), putRequestedFor(urlEqualTo("/some/endpoint")));
    }

    @Test
    public void testDelete() {
        wm.stubFor(delete(urlEqualTo("/some/endpoint"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 200, \"message\": \"deleted\"}")));

        int result = SgctlTool.exec("rest", "delete", "/some/endpoint", "-i", "invalid_path", "--json", "invalid_json",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(1), deleteRequestedFor(urlEqualTo("/some/endpoint")));
    }

    @Test
    public void testPost() {
        wm.stubFor(post(urlEqualTo("/some/endpoint"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":200, \"message\": \"success\"}")));

        int result = SgctlTool.exec("rest", "post", "/some/endpoint",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(1), postRequestedFor(urlEqualTo("/some/endpoint")));

        wm.stubFor(post(urlEqualTo("/some/endpoint"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"do\": \"something\"}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":200, \"message\": \"success\"}")));

        result = SgctlTool.exec("rest", "post", "/some/endpoint", "--json", "{\"do\":\"something\"}",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(2), postRequestedFor(urlEqualTo("/some/endpoint")));
    }

    @Test
    public void testPatch() {
        wm.stubFor(patch(urlEqualTo("/some/endpoint"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"patch\":\"something\"}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":200, \"message\":\"success\"}")));

        int result = SgctlTool.exec("rest", "patch", "/some/endpoint", "--json", "{\"patch\":\"something\"}",
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");
        Assertions.assertEquals(0, result);
        wm.verify(exactly(1), patchRequestedFor(urlEqualTo("/some/endpoint")));
    }
}
