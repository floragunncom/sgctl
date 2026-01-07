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
 * Negative-path tests for {@link UserConfigReader}.
 */
class UserConfigReaderTest {

    /**
     * Verifies a non-object user entry is reported as an invalid type.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidUserEntryType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path userFile = tempDir.resolve("user.json");
        Files.writeString(userFile, "{\"user1\":\"oops\"}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new UserConfigReader(userFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("user.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "origin".equals(entry.getParameter())));
    }

    /**
     * Verifies unknown keys are recorded for user entries.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportUnknownUserKeys(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path userFile = tempDir.resolve("user.json");
        Files.writeString(userFile, "{\"user1\":{\"username\":\"user1\",\"roles\":[],\"enabled\":true,\"metadata\":{},\"extra\":1}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new UserConfigReader(userFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("user.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "extra".equals(entry.getParameter())));
    }

    /**
     * Verifies missing required parameters are reported.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportMissingRequiredUserFields(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path userFile = tempDir.resolve("user.json");
        Files.writeString(userFile, "{\"user1\":{\"username\":\"user1\",\"metadata\":{}}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new UserConfigReader(userFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("user.json", MigrationReport.Category.MANUAL)
                .stream()
                .anyMatch(entry -> "enabled".equals(entry.getParameter())));
        assertTrue(report.getEntries("user.json", MigrationReport.Category.MANUAL)
                .stream()
                .anyMatch(entry -> "roles".equals(entry.getParameter())));
    }
}
