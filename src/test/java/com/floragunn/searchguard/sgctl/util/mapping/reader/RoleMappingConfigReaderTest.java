/*
 * Copyright 2025-2026 floragunn GmbH
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
 * Negative-path tests for {@link RoleMappingConfigReader}.
 */
class RoleMappingConfigReaderTest {

    /**
     * Verifies missing roles/role_templates are reported.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportMissingRoles(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path mappingFile = tempDir.resolve("role_mapping.json");
        Files.writeString(mappingFile, "{\"mapping1\":{\"enabled\":true}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new RoleMappingConfigReader(mappingFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("role_mapping.json", MigrationReport.Category.MANUAL)
                .stream()
                .anyMatch(entry -> "roles / role_templates".equals(entry.getParameter())));
    }

    /**
     * Verifies invalid roles types are reported.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidRolesType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = ReportTestSupport.newReport();
        Path mappingFile = tempDir.resolve("role_mapping.json");
        Files.writeString(mappingFile, "{\"mapping1\":{\"roles\":\"not-a-list\"}}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            new RoleMappingConfigReader(mappingFile.toFile(), new IntermediateRepresentation());
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("role_mapping.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "mapping1".equals(entry.getParameter())));
    }
}
