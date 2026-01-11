package com.floragunn.searchguard.sgctl.testsupport;

import org.junit.jupiter.api.Assumptions;

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
}
