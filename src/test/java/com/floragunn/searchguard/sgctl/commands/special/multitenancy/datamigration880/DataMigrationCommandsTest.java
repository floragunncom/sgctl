/*
 * Copyright 2023 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands.special.multitenancy.datamigration880;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.certificate.TestCertificates;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DataMigrationCommandsTest {

    private static final String ENDPOINT_PATH = "/_searchguard/config/fe_multi_tenancy/data_migration/8_8_0";
    private static final TestCertificates TEST_CERTIFICATES = TestCertificates.builder()
            .ca("CN=localhost,OU=SearchGuard,O=SearchGuard")
            .addAdminClients(singletonList("CN=admin-0.example.com,OU=SearchGuard,O=SearchGuard"), 10, "secret").build();
    private static final TestCertificate WM_CERT = TEST_CERTIFICATES.create("CN=localhost,OU=SearchGuard,O=SearchGuard");

    private static String configDir;
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @RegisterExtension
    private final static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig()
            .httpDisabled(true)
            .dynamicHttpsPort()
            .keystorePath(WM_CERT.getJksFile().getPath())
            .keystorePassword(WM_CERT.getPrivateKeyPassword())
            .keyManagerPassword(WM_CERT.getPrivateKeyPassword())
    ).build();

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

    @BeforeAll
    public static void connect() throws Exception {
        configDir = Files.createTempDirectory("sgctl-test-config").toString();
        int result = SgctlTool.exec("connect", "-h", "localhost", "-p", String.valueOf(wm.getHttpsPort()),
                "--cert", TEST_CERTIFICATES.getAdminCertificate().getCertificateFile().getPath(),
                "--key", TEST_CERTIFICATES.getAdminCertificate().getPrivateKeyFile().getPath(),
                "--key-pass", "secret",
                "--ca-cert", TEST_CERTIFICATES.getCaCertificate().getCertificateFile().getPath(),
                "--sgctl-config-dir", configDir, "--debug", "--skip-connection-check");

        assertThat(result, equalTo(0));
    }

    @Test
    public void getMigrationState_shouldGetMigrationState() {
        final String response = DocNode.of("status", HttpStatus.SC_OK, "migration-status", "ok").toPrettyJsonString();
        mockResponse(RequestMethod.GET, HttpStatus.SC_OK, response);

        int result = SgctlTool.exec("special", "get-mt-data-migration-state-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        assertThat(result, equalTo(0));
        assertThat(outputStreamCaptor.toString(), containsString("Getting state of multi-tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(outputStreamCaptor.toString(), containsString(response));
        assertThat(errStreamCaptor.toString().isEmpty(), equalTo(true));
    }

    @Test
    public void getMigrationState_shouldNotGetMigrationState_response_404() {
        final String response = DocNode.of("status", HttpStatus.SC_NOT_FOUND).toPrettyJsonString();
        mockResponse(RequestMethod.GET, HttpStatus.SC_NOT_FOUND, response);
        int result = SgctlTool.exec("special", "get-mt-data-migration-state-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        assertThat(result, equalTo(1));
        assertThat(outputStreamCaptor.toString(), containsString("Getting state of multi-tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(errStreamCaptor.toString(), containsString("Not Found"));
    }

    @Test
    public void getMigrationState_shouldNotGetMigrationState_response_500() {
        final String response = DocNode.of("status", HttpStatus.SC_INTERNAL_SERVER_ERROR).toPrettyJsonString();
        mockResponse(RequestMethod.GET, HttpStatus.SC_INTERNAL_SERVER_ERROR, response);

        int result = SgctlTool.exec("special", "get-mt-data-migration-state-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        assertThat(result, equalTo(1));
        assertThat(outputStreamCaptor.toString(), containsString("Getting state of multi-tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(errStreamCaptor.toString(), containsString("Server Error"));
    }

    @Test
    public void startMigration_shouldStartMigration_withoutMigrationOptions() {
        final String response = DocNode.of("status", HttpStatus.SC_OK, "migration-status", "started").toPrettyJsonString();
        mockResponse(RequestMethod.POST, HttpStatus.SC_OK, response);

        int result = SgctlTool.exec("special", "start-mt-data-migration-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        String expectedRequestBody = DocNode.of("allow_yellow_indices", false).toPrettyJsonString();

        assertThat(result, equalTo(0));
        assertThat(outputStreamCaptor.toString(), containsString("Starting multi tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(outputStreamCaptor.toString(), containsString(response));
        assertThat(outputStreamCaptor.toString(), containsString("Request body: " + expectedRequestBody));
        assertThat(errStreamCaptor.toString().isEmpty(), equalTo(true));
        wm.verify(1, postRequestedFor(urlEqualTo(ENDPOINT_PATH))
                .withRequestBody(new EqualToPattern(expectedRequestBody))
                .withHeader("Content-Type", new EqualToPattern("application/json")));
    }

    @Test
    public void startMigration_shouldStartMigration_withAllowYellowIndicesMigrationOption() {
        final String response = DocNode.of("status", HttpStatus.SC_OK, "migration-status", "started").toPrettyJsonString();
        mockResponse(RequestMethod.POST, HttpStatus.SC_OK, response);

        int result = SgctlTool.exec("special", "start-mt-data-migration-from-8.7", "--allow-yellow-indices", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        String expectedRequestBody = DocNode.of("allow_yellow_indices", true).toPrettyJsonString();

        assertThat(result, equalTo(0));
        assertThat(outputStreamCaptor.toString(), containsString("Starting multi tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(outputStreamCaptor.toString(), containsString(response));
        assertThat(outputStreamCaptor.toString(), containsString("Request body: " + expectedRequestBody));
        assertThat(errStreamCaptor.toString().isEmpty(), equalTo(true));
        wm.verify(1, postRequestedFor(urlEqualTo(ENDPOINT_PATH))
                .withRequestBody(new EqualToPattern(expectedRequestBody))
                .withHeader("Content-Type", new EqualToPattern("application/json")));
    }

    @Test
    public void startMigration_shouldNotStartMigration_response_404() {
        final String response = DocNode.of("status", HttpStatus.SC_NOT_FOUND).toPrettyJsonString();
        mockResponse(RequestMethod.POST, HttpStatus.SC_NOT_FOUND, response);

        int result = SgctlTool.exec("special", "start-mt-data-migration-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        assertThat(result, equalTo(1));
        assertThat(outputStreamCaptor.toString(), containsString("Starting multi tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(errStreamCaptor.toString(), containsString("Not Found"));
    }

    @Test
    public void startMigration_shouldNotStartMigration_response_500() {
        final String response = DocNode.of("status", HttpStatus.SC_INTERNAL_SERVER_ERROR, "migration-status", "started").toPrettyJsonString();
        mockResponse(RequestMethod.POST, HttpStatus.SC_INTERNAL_SERVER_ERROR, response);

        int result = SgctlTool.exec("special", "start-mt-data-migration-from-8.7", "--sgctl-config-dir", configDir,
                "--debug", "--skip-connection-check");

        assertThat(result, equalTo(1));
        assertThat(outputStreamCaptor.toString(), containsString("Starting multi tenancy data migration from Kibana 8.7 to 8.8"));
        assertThat(errStreamCaptor.toString(), containsString("Server Error"));
    }

    private void mockResponse(RequestMethod requestMethod, int status, String body) {
        wm.stubFor(WireMock.request(requestMethod.getName(), urlEqualTo(ENDPOINT_PATH))
                .willReturn(aResponse().withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body))
        );
    }
}
