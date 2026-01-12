package com.floragunn.searchguard.sgctl.util.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MigrationReport} behavior and reporting output.
 */
class MigrationReportTest {

    private MigrationReport report;
    private ByteArrayOutputStream out;
    private PrintStream printStream;

    /**
     * Sets up an isolated report with a fresh output stream.
     */
    @BeforeEach
    void setUp() {
        report = MigrationReport.shared;
        report.clear();
        out = new ByteArrayOutputStream();
        printStream = new PrintStream(out);
    }

    /**
     * Closes the report output stream.
     */
    @AfterEach
    void tearDown() {
        printStream.close();
    }

    /**
     * Verifies unknown key warnings use the correct preset and message.
     */
    @Test
    void addUnknownKeyCreatesPresetWarning() {
        report.addUnknownKey("file1", "key1", "path1");

        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("key1", entry.getParameter());
        assertEquals("Encountered unknown key 'key1' at path 'path1'", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.UNKNOWN_KEY, entry.getPreset());
    }

    /**
     * Verifies invalid type warnings are created for concrete and null values.
     */
    @Test
    void addInvalidTypeHandlesConcreteAndNullValues() {
        report.addInvalidType("file1", "path1", String.class, 123);
        report.addInvalidType("file1", "pathNull", String.class, null);

        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(2, entries.size());
        assertEquals("path1", entries.get(0).getParameter());
        assertEquals("Expected type 'java.lang.String' for 'path1', but found 'java.lang.Integer'", entries.get(0).getMessage());
        assertEquals("pathNull", entries.get(1).getParameter());
        assertEquals("Expected type 'java.lang.String' for 'pathNull', but found 'null'", entries.get(1).getMessage());
        assertEquals(MigrationReport.ReportPreset.INVALID_TYPE, entries.get(0).getPreset());
        assertEquals(MigrationReport.ReportPreset.INVALID_TYPE, entries.get(1).getPreset());
    }

    /**
     * Verifies missing parameter errors are recorded as manual entries.
     */
    @Test
    void addMissingParameterCreatesManualEntry() {
        report.addMissingParameter("file1", "param1", "path1");

        var entries = report.getEntries("file1", MigrationReport.Category.MANUAL);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("param1", entry.getParameter());
        assertEquals("'path1' missing required parameter 'param1'", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.MISSING_PARAMETER, entry.getPreset());
    }

    /**
     * Verifies ignored keys create warnings with the ignored preset.
     */
    @Test
    void addIgnoredKeyCreatesWarningEntry() {
        report.addIgnoredKey("file1", "key1", "path1");

        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("key1", entry.getParameter());
        assertEquals("Key 'key1' at path 'path1' is ignored for migration", entry.getMessage());
        assertEquals(MigrationReport.ReportPreset.IGNORED_KEY, entry.getPreset());
    }

    /**
     * Verifies migrated entries support old/new and old-only parameters.
     */
    @Test
    void addMigratedSupportsOldAndNewParameters() {
        report.addMigrated("file1", "oldParam", "newParam");
        report.addMigrated("file1", "onlyOld");

        var entries = report.getEntries("file1", MigrationReport.Category.MIGRATED);
        assertEquals(2, entries.size());
        var withNew = entries.get(0);
        assertEquals("oldParam", withNew.getParameter());
        assertEquals("newParam", withNew.getNewParameter());
        assertNull(withNew.getMessage());
        var withoutNew = entries.get(1);
        assertEquals("onlyOld", withoutNew.getParameter());
        assertNull(withoutNew.getNewParameter());
        assertNull(withoutNew.getMessage());
    }

    /**
     * Verifies warning and manual entries do not set report presets.
     */
    @Test
    void addWarningAndManualActionDoNotSetPreset() {
        report.addWarning("file1", "param1", "Check this");
        report.addManualAction("file1", "param2", "Do manually");

        var warnings = report.getEntries("file1", MigrationReport.Category.WARNING);
        var manuals = report.getEntries("file1", MigrationReport.Category.MANUAL);
        assertEquals(1, warnings.size());
        assertEquals("param1", warnings.get(0).getParameter());
        assertEquals("Check this", warnings.get(0).getMessage());
        assertNull(warnings.get(0).getPreset());
        assertEquals(1, manuals.size());
        assertEquals("param2", manuals.get(0).getParameter());
        assertEquals("Do manually", manuals.get(0).getMessage());
        assertNull(manuals.get(0).getPreset());
    }

    /**
     * Verifies clear() resets entries and counts.
     */
    @Test
    void clearResetsStateAndCounts() {
        report.addMigrated("file1", "old", "new");
        report.addWarning("file1", "p", "warn");
        assertEquals(1, report.count(MigrationReport.Category.MIGRATED));
        assertEquals(1, report.count("file1", MigrationReport.Category.WARNING));

        report.clear();

        assertTrue(report.getEntries("file1", MigrationReport.Category.MIGRATED).isEmpty());
        assertEquals(0, report.count(MigrationReport.Category.MIGRATED));
        assertEquals(0, report.count("file1", MigrationReport.Category.WARNING));
    }

    /**
     * Verifies report output includes headers and sections for populated entries.
     */
    @Test
    void printReportIncludesHeaderAndSections() {
        report.addMigrated("file1", "old", "new");
        report.addWarning("file1", "w1", "Check");
        report.addManualAction("file1", "m1", "Do manually");

        report.printReport(printStream);

        String output = output();
        assertTrue(output.contains("---------- Migration Report ----------"));
        assertTrue(output.contains("MIGRATED (1)"));
        assertTrue(output.contains("WARNING (1)"));
        assertTrue(output.contains("MANUAL (1)"));
        assertTrue(output.contains("old -> new"));
        assertTrue(output.contains("Check"));
        assertTrue(output.contains("Do manually"));
        assertTrue(output.contains("---------- End Migration Report ----------"));
    }

    /**
     * Verifies report output remains valid when no entries exist.
     */
    @Test
    void printReportWithNoEntriesStillHasHeaderAndFooter() {
        report.printReport(printStream);

        String output = output();
        assertTrue(output.contains("---------- Migration Report ----------"));
        assertTrue(output.contains("---------- End Migration Report ----------"));
    }

    /**
     * Verifies report output groups preset warnings before free-form warnings.
     */
    @Test
    void presetsAreGroupedAndOrderedBeforeFreeWarnings() {
        report.addWarning("file1", "freeWarn", "a free warning");
        report.addUnknownKey("file1", "ukey1", "p");
        report.addInvalidType("file1", "intPath1", Integer.class, "abc");
        report.addUnknownKey("file1", "ukey2", "p");
        report.addIgnoredKey("file1", "ikey1", "p");
        report.addInvalidType("file1", "intPath2", Integer.class, 42);
        report.addWarning("file1", "freeWarn2", "another free warning");

        report.printReport(printStream);

        int idxUkey1 = indexOfLineContaining("ukey1");
        int idxUkey2 = indexOfLineContaining("ukey2");
        int idxInt1 = indexOfLineContaining("intPath1");
        int idxInt2 = indexOfLineContaining("intPath2");
        int idxIkey1 = indexOfLineContaining("ikey1");
        int idxFree1 = indexOfLineContaining("a free warning");
        int idxFree2 = indexOfLineContaining("another free warning");

        assertTrue(idxUkey1 >= 0 && idxUkey2 >= 0 && idxInt1 >= 0 && idxInt2 >= 0 && idxIkey1 >= 0);

        int firstUnknown = Math.min(idxUkey1, idxUkey2);
        int firstInvalid = Math.min(idxInt1, idxInt2);
        assertTrue(firstUnknown < firstInvalid, "UNKNOWN_KEY entries should come before INVALID_TYPE entries");
        assertTrue(firstInvalid < idxIkey1, "INVALID_TYPE entries should come before IGNORED_KEY entries");

        int lastPresetPos = Math.max(Math.max(idxUkey1, idxUkey2), Math.max(Math.max(idxInt1, idxInt2), idxIkey1));
        assertTrue(lastPresetPos < idxFree1 && lastPresetPos < idxFree2, "free warnings should appear after preset groups");
    }

    /**
     * Verifies warning groups are contiguous within report output.
     */
    @Test
    void presetEntriesAreContiguous() {
        report.addInvalidType("file1", "I1", Integer.class, "x");
        report.addUnknownKey("file1", "U1", "p");
        report.addInvalidType("file1", "I2", Integer.class, 12);
        report.addUnknownKey("file1", "U2", "p");
        report.addIgnoredKey("file1", "G1", "p");
        report.addUnknownKey("file1", "U3", "p");
        report.addInvalidType("file1", "I3", Integer.class, null);

        report.printReport(printStream);

        List<Integer> unknownIdxs = List.of(
            indexOfLineContaining("U1"),
            indexOfLineContaining("U2"),
            indexOfLineContaining("U3")
        );
        List<Integer> invalidIdxs = List.of(
            indexOfLineContaining("I1"),
            indexOfLineContaining("I2"),
            indexOfLineContaining("I3")
        );
        List<Integer> ignoredIdxs = List.of(indexOfLineContaining("G1"));

        unknownIdxs.forEach(i -> assertTrue(i >= 0, "unknown entry not found"));
        invalidIdxs.forEach(i -> assertTrue(i >= 0, "invalid entry not found"));
        ignoredIdxs.forEach(i -> assertTrue(i >= 0, "ignored entry not found"));

        int uMin = unknownIdxs.stream().mapToInt(Integer::intValue).min().getAsInt();
        int uMax = unknownIdxs.stream().mapToInt(Integer::intValue).max().getAsInt();
        int iMin = invalidIdxs.stream().mapToInt(Integer::intValue).min().getAsInt();
        int iMax = invalidIdxs.stream().mapToInt(Integer::intValue).max().getAsInt();
        int gMin = ignoredIdxs.stream().mapToInt(Integer::intValue).min().getAsInt();

        assertTrue(uMax < iMin, "All UNKNOWN entries should end before first INVALID entry");
        assertTrue(iMax < gMin, "All INVALID entries should end before first IGNORED entry");

        String betweenU = output().substring(lineStartOffset(uMin), lineStartOffset(uMax));
        assertFalse(betweenU.contains("I1") || betweenU.contains("I2") || betweenU.contains("I3") || betweenU.contains("G1"),
            "Interleaving detected inside UNKNOWN block");

        String betweenI = output().substring(lineStartOffset(iMin), lineStartOffset(iMax));
        assertFalse(betweenI.contains("U1") || betweenI.contains("U2") || betweenI.contains("U3") || betweenI.contains("G1"),
            "Interleaving detected inside INVALID block");
    }

    /**
     * Verifies entries are isolated per file.
     */
    @Test
    void multipleFilesRemainIsolated() {
        report.addMigrated("fileA", "pA", "npA");
        report.addWarning("fileB", "pB", "warnB");

        assertEquals(1, report.getEntries("fileA", MigrationReport.Category.MIGRATED).size());
        assertEquals(0, report.getEntries("fileA", MigrationReport.Category.WARNING).size());
        assertEquals(1, report.getEntries("fileB", MigrationReport.Category.WARNING).size());
    }

    /**
     * Verifies snapshot immutability and content.
     */
    @Test
    void snapshotIsImmutableAndReflectsState() {
        report.addWarning("fileX", "p1", "m1");

        var snapshot = report.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey("fileX"));
        assertEquals(1, snapshot.get("fileX").get(MigrationReport.Category.WARNING).size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("foo", null));
    }

    /**
     * Verifies entry equality and string output include core fields.
     */
    @Test
    void entryEqualityAndToStringReflectFields() {
        report.addWarning("file1", "p", "m");

        var e = report.getEntries("file1", MigrationReport.Category.WARNING).get(0);
        var same = new MigrationReport.Entry("p", "m", null, null);
        assertEquals(e, same);
        assertEquals(e.hashCode(), same.hashCode());
        assertTrue(e.toString().contains("parameter='p'"));
    }

    /**
     * Finds the line index containing a token.
     *
     * @param token token to search for
     * @return index of the line containing the token, or -1
     */
    private int indexOfLineContaining(String token) {
        List<String> lines = outputLines();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(token)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Computes the starting offset of a line in the output.
     *
     * @param lineIndex index of line in the output
     * @return character offset for the line start
     */
    private int lineStartOffset(int lineIndex) {
        int offset = 0;
        List<String> lines = outputLines();
        for (int i = 0; i < lineIndex && i < lines.size(); i++) {
            offset += lines.get(i).length() + 1; // +1 accounts for line break
        }
        return offset;
    }

    /**
     * Splits the output into lines.
     *
     * @return list of output lines
     */
    private List<String> outputLines() {
        return output().lines().toList();
    }

    /**
     * Returns the captured report output.
     *
     * @return output as string
     */
    private String output() {
        return out.toString();
    }
}
