package com.floragunn.searchguard.sgctl.testsupport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

import com.floragunn.searchguard.test.helper.certificate.TestCertificate;
import com.floragunn.searchguard.test.helper.cluster.LocalCluster;

/**
 * Shared gating for integration tests that require a running Search Guard cluster.
 */
public final class ExternalTestSupport {
    private static final String EXTERNAL_TESTS_ENV = "SGCTL_EXTERNAL_TESTS";

    private ExternalTestSupport() {
    }

    public static boolean isExternalTestsEnabled() {
        return Boolean.getBoolean("sgctl.externalTests")
                || "true".equalsIgnoreCase(System.getenv(EXTERNAL_TESTS_ENV));
    }

    public static void assumeExternalTestsEnabled() {
        Assumptions.assumeTrue(isExternalTestsEnabled(),
                "Set -Dsgctl.externalTests=true or SGCTL_EXTERNAL_TESTS=true to run integration tests");
    }

    /**
     * Builds sgctl connect arguments for a locally started cluster.
     */
    public static String[] buildConnectArgs(LocalCluster cluster, String configDir, boolean skipConnectionCheck) {
        InetSocketAddress httpAddress = cluster.getHttpAddress();
        TestCertificate adminCertificate = cluster.getTestCertificates().getAdminCertificate();
        String adminCert = adminCertificate.getCertificateFile().getPath();
        String adminKey = adminCertificate.getPrivateKeyFile().getPath();
        String rootCaCert = cluster.getTestCertificates().getCaCertFile().getPath();

        List<String> args = new ArrayList<>();
        args.add("connect");
        args.add("-h");
        args.add(httpAddress.getHostString());
        args.add("-p");
        args.add(String.valueOf(httpAddress.getPort()));
        args.add("--cert");
        args.add(adminCert);
        args.add("--key");
        args.add(adminKey);

        String keyPass = adminCertificate.getPrivateKeyPassword();
        if (keyPass != null && !keyPass.isEmpty()) {
            args.add("--key-pass");
            args.add(keyPass);
        }

        args.add("--ca-cert");
        args.add(rootCaCert);
        args.add("--insecure");
        args.add("--debug");
        args.add("--sgctl-config-dir");
        args.add(configDir);

        if (skipConnectionCheck) {
            args.add("--skip-connection-check");
        }

        return args.toArray(new String[0]);
    }
}
