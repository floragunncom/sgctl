package com.floragunn.searchguard.sgctl.util.mapping.testsupport;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

/**
 * Helper methods for creating isolated {@link MigrationReport} instances in tests.
 */
public final class ReportTestSupport {
    private ReportTestSupport() {
    }

    /**
     * Creates a new report instance without using the shared singleton.
     *
     * @return fresh migration report
     */
    public static MigrationReport newReport() {
        try {
            var ctor = MigrationReport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create isolated MigrationReport", e);
        }
    }
}
