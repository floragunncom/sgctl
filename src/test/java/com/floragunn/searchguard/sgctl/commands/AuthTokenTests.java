/*
 * Copyright 2021-2022 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands;

import static java.util.Collections.singletonList;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;


import com.floragunn.searchguard.authtoken.AuthTokenModule;
import com.floragunn.searchguard.sgctl.SgctlTool;
import com.floragunn.searchguard.test.TestSgConfig;
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
import picocli.CommandLine.ExitCode;


public class AuthTokenTests {
    static final TestSgConfig.DlsFls DLSFLS = new TestSgConfig.DlsFls().useImpl("flx").metrics("detailed");
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();
    static final TestSgConfig.User ADMIN = new TestSgConfig.User("admin")
            .roles(new TestSgConfig.Role("all_access").indexPermissions("*").on("*").clusterPermissions("*")

            );
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

    private static LocalCluster cluster;
    static String configDir;

    @BeforeAll
    public static void connect() throws Exception {
        final TestSgConfig.Authc AUTHC = new TestSgConfig.Authc(new TestSgConfig.Authc.Domain("basic/internal_users_db"));
        final TestSgConfig.AuthTokenService AUTH_TOKEN_SERVICE = new TestSgConfig.AuthTokenService().enabled(true).jwtSigningKeyHs512(
                "eTDZjSqRD9Abhod9iqeGX_7o93a-eElTeXWAF6FmzQshmRIrPD-C9ET3pFjJ_IBrzmWIZDk8ig-X_PIyGmKsxNMsrU-0BNWF5gJq5xOp4rYTl8z66Tw9wr8tHLxLxgJqkLSuUCRBZvlZlQ7jNdhBBxgM-hdSSzsN1T33qdIwhrUeJ-KXI5yKUXHjoWFYb9tETbYQ4NvONowkCsXK_flp-E3F_OcKe_z5iVUszAV8QfCod1zhbya540kDejXCL6N_XMmhWJqum7UJ3hgf6DEtroPSnVpHt4iR5w9ArKK-IBgluPght03gNcoNqwz7p77TFbdOmUKF_PWy1bcdbaUoSg");
        final String INDEX = "logs";

        TestSgConfig.AuthTokenService authTokenService = new TestSgConfig.AuthTokenService();
        cluster = new LocalCluster.Builder().singleNode().enterpriseModulesEnabled().enableModule(AuthTokenModule.class).authTokenService(AUTH_TOKEN_SERVICE).users(ADMIN).authc(AUTHC).dlsFls(DLSFLS).sslEnabled(TEST_CERTIFICATES).start();
        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();
        configDir = Files.createTempDirectory("sgctl-test-config").toString();

        int rc = SgctlTool.exec("connect", "-h", httpAddress.getHostString(), "-p", String.valueOf(httpAddress.getPort()), "--cert", adminCert,
                "--key", adminKey, "--key-pass", "secret", "--ca-cert", rootCaCert, "--debug", "--sgctl-config-dir", configDir);

        Assertions.assertEquals(0, rc);
    }

    @Test
    public void testAuthTokens_testListTokens() throws Exception {

        int result = SgctlTool.exec("list-auth-tokens", "--sgctl-config-dir", configDir, "--debug");

        Assertions.assertEquals(ExitCode.OK, result);
        Assertions.assertTrue(outputStreamCaptor.toString().contains("\"timed_out\" : false,"));

    }

    @Test
    public void testAuthTokens_testRevokeToken() throws Exception {
            String tokenId = "NonExistinTokenId";

            int result = SgctlTool.exec("revoke-auth-token","--sgctl-config-dir", configDir, "--debug", tokenId);

            Assertions.assertEquals(ExitCode.OK, result);
            Assertions.assertTrue(outputStreamCaptor.toString().contains("Auth token has been revoked") || outputStreamCaptor.toString().contains("No such auth token") );
    }

    @Test
    public void testAuthTokens_RevokeTokenShouldFailWhenNoTokenProvided() throws Exception {

        int result = SgctlTool.exec("revoke-auth-token","--sgctl-config-dir", configDir, "--debug");

        Assertions.assertEquals(ExitCode.USAGE, result);
        Assertions.assertTrue(errStreamCaptor.toString().contains("Missing required parameter: '<authTokenId>'"));
    }

    @AfterAll
    public static void destroy() throws Exception {
        cluster.close();
    }
}