package com.floragunn.searchguard.sgctl.testsupport;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

class MigrationReportTest extends TestBase{
    private MigrationReport report;

    @BeforeEach
    void setUp(){
        report = MigrationReport.shared;
        report.clear();
    }

    @Test
    void testAddUnknownKey(){
        report.addUnknownKey("file1", "key1", "path1");
        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("key1", entry.getParameter());
        assertEquals("Encountered unknown key 'key1' at path 'path1'", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.UNKNOWN_KEY, entry.getPreset());
    }
    @Test
    void testAddInvalidType() {
        report.addInvalidType("file1", "path1", String.class, 123);
        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("path1", entry.getParameter());
        assertEquals("Expected type 'java.lang.String' for 'path1', but found 'java.lang.Integer'", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.INVALID_TYPE, entry.getPreset());
    }
    @Test
    void testAddMissingParameter() {
        report.addMissingParameter("file1", "param1", "path1");
        var entries = report.getEntries("file1", MigrationReport.Category.MANUAL);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("param1", entry.getParameter());
        assertEquals("'path1' missing required parameter 'param1'", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.MISSING_PARAMETER, entry.getPreset());
    }
    @Test
    void testAddIgnoredKey() {
        report.addIgnoredKey("file1", "key1", "path1");
        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("key1", entry.getParameter());
        assertEquals("Key 'key1' at path 'path1' is ignored for migration", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.IGNORED_KEY, entry.getPreset());
    }
    @Test
    void testAddMigrated() {
        report.addMigrated("file1", "oldParam", "newParam");
        var entries = report.getEntries("file1", MigrationReport.Category.MIGRATED);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("oldParam", entry.getParameter());
        assertEquals("newParam", entry.getNewParameter());
        assertNull(entry.getMessage());
        assertNull(entry.getPreset());
    }
    @Test
    void testAddWarning() {
        report.addWarning("file1", "param1", "Check this");
        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("param1", entry.getParameter());
        assertEquals("Check this", entry.getMessage());
        assertNull(entry.getPreset());
    }
    @Test
    void testClear() {
        report.addMigrated("file1", "old", "new");
        assertFalse(report.getEntries("file1", MigrationReport.Category.MIGRATED).isEmpty());
        report.clear();
        assertTrue(report.getEntries("file1", MigrationReport.Category.MIGRATED).isEmpty());
        assertEquals(0, report.count(MigrationReport.Category.MIGRATED));
    }
    @Test
    void testAddManualAction() {
        report.addManualAction("file1", "param1", "Do manually");
        var entries = report.getEntries("file1", MigrationReport.Category.MANUAL);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("param1", entry.getParameter());
        assertEquals("Do manually", entry.getMessage());
        assertNull(entry.getPreset());
    }
    @Test
    void testPrintReport() {
        report.addMigrated("file1", "old", "new");
        report.addWarning("file1", "w1", "Check");
        report.addManualAction("file1", "m1", "Do manually");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outContent);

        report.printReport(printStream);

        String output = outContent.toString();
        assertTrue(output.contains("MIGRATED (1)"));
        assertTrue(output.contains("Parameters that have been successfully migrated"));
        assertTrue(output.contains("WARNING (1)"));
        assertTrue(output.contains("MANUAL (1)"));
        assertTrue(output.contains("old -> new"));
        assertTrue(output.contains("Do manually"));
        assertTrue(output.contains("w1"));
        assertTrue(output.contains("m1"));
    }
    
}
