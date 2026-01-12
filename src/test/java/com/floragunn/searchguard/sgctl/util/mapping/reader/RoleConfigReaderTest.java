package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.testsupport.ReportTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negative-path tests for {@link RoleConfigReader}.
 */
class RoleConfigReaderTest {

    /**
     * Verifies a non-object JSON root is reported as an invalid type.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidRootType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path roleFile = tempDir.resolve("role.json");
        Files.writeString(roleFile, "[\"not-an-object\"]");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new RoleConfigReader(roleFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("role.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "origin".equals(entry.parameter())));
    }

    /**
     * Verifies invalid cluster types are reported.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidClusterType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path roleFile = tempDir.resolve("role.json");
        Files.writeString(roleFile, "{\"role1\":{\"cluster\":\"oops\"}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new RoleConfigReader(roleFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("role.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "role1".equals(entry.parameter())));
    }

    /**
     * Verifies unknown keys are recorded.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportUnknownRoleKeys(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path roleFile = tempDir.resolve("role.json");
        Files.writeString(roleFile, "{\"role1\":{\"unknown\":123}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new RoleConfigReader(roleFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("role.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "unknown".equals(entry.parameter())));
    }
}
