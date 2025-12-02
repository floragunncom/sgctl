package com.floragunn.searchguard.sgctl.testsupport;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void testAddInvalidType_foundNull() {
        report.addInvalidType("file1", "pathNull", String.class, null);
        var entries = report.getEntries("file1", MigrationReport.Category.WARNING);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("pathNull", entry.getParameter());
        assertEquals("Expected type 'java.lang.String' for 'pathNull', but found 'null'", entry.getMessage());
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
    void testAddMigratedWithoutNewParameter() {
        report.addMigrated("file1", "onlyOld");
        var entries = report.getEntries("file1", MigrationReport.Category.MIGRATED);
        assertEquals(1, entries.size());
        var entry = entries.get(0);
        assertEquals("onlyOld", entry.getParameter());
        assertNull(entry.getNewParameter());
        assertNull(entry.getMessage());
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
        assertEquals(1, report.count(MigrationReport.Category.MIGRATED));
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
    @Test
    void testPresetsAreGroupedAndOrdered() {
        report.addWarning("file1", "freeWarn", "a free warning");               
        report.addUnknownKey("file1", "ukey1", "p");                          
        report.addInvalidType("file1", "intPath1", Integer.class, "abc");     
        report.addUnknownKey("file1", "ukey2", "p");                         
        report.addIgnoredKey("file1", "ikey1", "p");                          
        report.addInvalidType("file1", "intPath2", Integer.class, 42);       
        report.addWarning("file1", "freeWarn2", "another free warning");      
    
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        report.printReport(new PrintStream(out));
        String output = out.toString();
    
        int idxUkey1 = output.indexOf("ukey1");
        int idxUkey2 = output.indexOf("ukey2");
        int idxInt1  = output.indexOf("intPath1");
        int idxInt2  = output.indexOf("intPath2");
        int idxIkey1 = output.indexOf("ikey1");
        int idxFree1 = output.indexOf("a free warning"); 
        int idxFree2 = output.indexOf("another free warning");
    
        
        assertTrue(idxUkey1 >= 0 && idxUkey2 >= 0 && idxInt1 >= 0 && idxInt2 >= 0 && idxIkey1 >= 0);
    
        
        int firstUnknown = Math.min(idxUkey1, idxUkey2);
        int firstInvalid = Math.min(idxInt1, idxInt2);
        assertTrue(firstUnknown < firstInvalid, "UNKNOWN_KEY entries should come before INVALID_TYPE entries");
    
        assertTrue(firstInvalid < idxIkey1, "INVALID_TYPE entries should come before IGNORED_KEY entries");
    
        int lastPresetPos = Math.max(Math.max(idxUkey1, idxUkey2), Math.max(Math.max(idxInt1, idxInt2), idxIkey1));
        assertTrue(lastPresetPos < idxFree1 && lastPresetPos < idxFree2, "free warnings should appear after preset groups");
    }
    
    @Test
    void testPresetEntriesAreContiguous_noInterleaving() {
        
        report.addInvalidType("file1", "I1", Integer.class, "x");
        report.addUnknownKey("file1", "U1", "p");
        report.addInvalidType("file1", "I2", Integer.class, 12);
        report.addUnknownKey("file1", "U2", "p");
        report.addIgnoredKey("file1", "G1", "p");
        report.addUnknownKey("file1", "U3", "p");
        report.addInvalidType("file1", "I3", Integer.class, null);
    
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        report.printReport(new PrintStream(out));
        String output = out.toString();
    
        List<Integer> unknownIdxs = List.of(
            output.indexOf("U1"),
            output.indexOf("U2"),
            output.indexOf("U3")
        );
        List<Integer> invalidIdxs = List.of(
            output.indexOf("I1"),
            output.indexOf("I2"),
            output.indexOf("I3")
        );
        List<Integer> ignoredIdxs = List.of(output.indexOf("G1"));
    
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
    
        String betweenU = output.substring(uMin, uMax);
        assertFalse(betweenU.contains("I1") || betweenU.contains("I2") || betweenU.contains("I3") || betweenU.contains("G1"),
            "Interleaving detected inside UNKNOWN block");
    
        String betweenI = output.substring(iMin, iMax);
        assertFalse(betweenI.contains("U1") || betweenI.contains("U2") || betweenI.contains("U3") || betweenI.contains("G1"),
            "Interleaving detected inside INVALID block");
    }
    
    @Test
    void testMultipleFilesIsolation() {
        report.addMigrated("fileA", "pA", "npA");
        report.addWarning("fileB", "pB", "warnB");
        assertEquals(1, report.getEntries("fileA", MigrationReport.Category.MIGRATED).size());
        assertEquals(0, report.getEntries("fileA", MigrationReport.Category.WARNING).size());
        assertEquals(1, report.getEntries("fileB", MigrationReport.Category.WARNING).size());
    }
    @Test
    void testGetSnapshotAndImmutability() {
        report.addWarning("fileX", "p1", "m1");
        var snapshot = report.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey("fileX"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("foo", null));
    }

    @Test
    void testEntryEqualsHashCodeToString() {
        report.addWarning("file1", "p", "m");
        var e = report.getEntries("file1", MigrationReport.Category.WARNING).get(0);
        var same = new MigrationReport.Entry("p", "m", null, null);
        assertEquals(e, same);
        assertEquals(e.hashCode(), same.hashCode());
        assertTrue(e.toString().contains("parameter='p'"));
    }
    
}
