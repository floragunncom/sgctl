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


package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.writer.RoleConfigWriter;
import org.jspecify.annotations.NonNull;
import picocli.CommandLine.Help.Ansi;

import java.io.PrintStream;
import java.util.*;


/**
 * Collects migration results and renders a readable report grouped by category.
 */
public class MigrationReport {
    public static MigrationReport shared = new MigrationReport();
    private static final String MANUAL_ACTION_TITLE = "  @|bold,yellow MANUAL ACTION REQUIRED (%d)|@\n  Parameters that could not be automatically migrated and require manual review or adjustment.\n";
    private static final String WARNING_TITLE = "  @|bold,yellow WARNINGS (%d)|@\n  Potentially problematic or ambiguous settings. Review them to ensure the migrated configuration behaves as expected.\n";
    private static final String MIGRATED_TITLE = "  @|bold,green SUCCESSFULLY MIGRATED (%d)|@\n  Parameters that have been successfully migrated.\n";
    private static final String INFO_TITLE = "  @|bold,blue INFO (%d)|@\n  General information and notes on behaviour.\n";
    private static final String DISPLAY_TEMPLATE = "    - %s\n      -> %s\n";
    private MigrationReport() {}
    private final LinkedHashMap<String, FileReport> files = new LinkedHashMap<>();
    private final RoleEntries roleEntries = new RoleEntries(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

    public enum Category {MIGRATED, WARNING, MANUAL, INFO}

    /* ----- public API ----- */
    public void addRoleEntry(RoleEntry entry) {
        if (entry.noIssues()) {
            roleEntries.successful.add(entry);
        } else if (entry.successful()) {
            roleEntries.withIssues.add(entry);
        } else {
            roleEntries.unsuccessful.add(entry);
        }
    }

    public void addUnknownKey(String file, String key, String path){
        addPreset(ReportPreset.UNKNOWN_KEY, Category.WARNING, file, key, key, path);
    }

    public <T> void addInvalidType(String file, String path, Class<T> expectedObject, Object foundObject){
        var expectedType = expectedObject.getTypeName();
        var foundType = "null";
        if (foundObject != null) {
            foundType = foundObject.getClass().getTypeName();
        }
        addPreset(ReportPreset.INVALID_TYPE, Category.WARNING, file, path, expectedType, path, foundType);
    }

    public void addMissingParameter(String file, String parameter, String path){
        addPreset(ReportPreset.MISSING_PARAMETER, Category.MANUAL, file, parameter, path, parameter);
    }

    public void addIgnoredKey(String file, String key, String path){
        addPreset(ReportPreset.IGNORED_KEY, Category.WARNING, file, key, key, path);
    }

    public void addInfo(String file, String info) {
        addPreset(ReportPreset.INFO, Category.INFO, file, info, info, info);
    }

    public void addMigrated(String file, String oldParameter, String newParameter){
        file(file).add(Category.MIGRATED, new Entry(oldParameter, null, newParameter, null));
    }

    public void addMigrated(String file, String parameter){
        addMigrated(file, parameter, null);
    }

    public void addWarning(String file, String parameter, String message){
        file(file).add(Category.WARNING, new Entry(parameter, message, null, null));
    }

    public void addManualAction(String file, String parameter, String message){
        file(file).add(Category.MANUAL, new Entry(parameter, message, null, null));
    }

    public void printReport(){
        printReport(System.out);
    }

    public void printReport(PrintStream out){
        out.println(applyFormating("@|bold ----------------------------- Migration Report -----------------------------|@"));
        for (Map.Entry<String, FileReport> fe : files.entrySet()) {
            out.println(applyFormating("@|bold File - " + fe.getKey() + ":|@\n"));
            FileReport fr = fe.getValue();
            printInfo(fr, out);
            printMigrated(fr, out);
            printWarnings(fr, out);
            printManuals(fr, out);
        }
        if (!(roleEntries.unsuccessful.isEmpty() && roleEntries.withIssues.isEmpty() && roleEntries.successful.isEmpty())) {
            out.println(applyFormating("@|bold File - " + RoleConfigWriter.FILE_NAME + ":|@\n"));
        }
        if (!roleEntries.successful.isEmpty()) {
            out.println(applyFormating("  @|bold,green SUCCESSFULLY MIGRATED (" + roleEntries.successful.size() + "):|@"));
            for (var entry : roleEntries.successful) out.println("    - " + entry.getName());
            out.println();
        }
        if (!roleEntries.withIssues.isEmpty()) {
            out.println(applyFormating("  @|bold,yellow MIGRATED WITH ISSUES (" + roleEntries.withIssues.size() + "):|@"));
            for (var entry : roleEntries.withIssues) entry.printEntry(out);
            out.println();
        }
        if (!roleEntries.unsuccessful.isEmpty()) {
            out.println(applyFormating("  @|bold,red NOT MIGRATED (" + roleEntries.unsuccessful.size() + "):|@"));
            for (var entry : roleEntries.unsuccessful) entry.printEntry(out);
            out.println();
        }
        out.println(applyFormating("@|bold ----------------------------- End Migration Report -----------------------------|@"));
    }

    /* ---------- internals ---------- */
    private FileReport file(String file){
        return files.computeIfAbsent(file, k -> new FileReport());
    }

    private void addPreset(ReportPreset rp,Category category, String file, String parameter, Object... args){
        String msg = rp.format(args);
        file(file).add(category, new Entry(parameter, msg, null, rp));
    }

    private void printInfo(FileReport fr, PrintStream out) {
        List<Entry> migrated = fr.get(Category.INFO);
        if(!migrated.isEmpty()){
            out.print(applyFormating(INFO_TITLE, migrated.size()));
            for(Entry e : migrated) out.printf("    - %s\n", e.parameter);
            out.println();
        }
    }

    void printMigrated(FileReport fr, PrintStream out){
        List<Entry> migrated = fr.get(Category.MIGRATED);
        if(!migrated.isEmpty()){
            out.print(applyFormating(MIGRATED_TITLE, migrated.size()));
            for(Entry e : migrated) {
                if(e.newParameter != null){
                    out.printf("    - %s -> %s\n", e.parameter, e.newParameter);
                } else {
                    out.printf("    - %s\n", e.parameter);
                }
            }
            out.println();
        }
    }

    void printWarnings(FileReport fr, PrintStream out){
        List<Entry> warnings = fr.get(Category.WARNING);
        if(!warnings.isEmpty()){
            out.printf(applyFormating(WARNING_TITLE, warnings.size()));
            printPresets(warnings, out);
            List<Entry> freeWarnings = new ArrayList<>();
            for (Entry e : warnings) {
                if (e.preset == null) freeWarnings.add(e);
            }
            if (!freeWarnings.isEmpty()) {
                for (Entry e : freeWarnings) {
                    out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
                }
                out.println();
            }
        }
    }

    void printPresets(List<Entry> entries, PrintStream out){
        for (ReportPreset rp : ReportPreset.values()) {
            List<Entry> presetWarnings = new ArrayList<>();
            for (Entry e : entries) {
                if (e.preset == rp) presetWarnings.add(e);
            }
            if (!presetWarnings.isEmpty()) {
                for (Entry e : presetWarnings) {
                    out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
                }
                out.println();
            }
        }
    }

    void printManuals(FileReport fr, PrintStream out){
        List<Entry> manuals = fr.get(Category.MANUAL);
        if(!manuals.isEmpty()){
            out.print(applyFormating(MANUAL_ACTION_TITLE, manuals.size()));
            for(Entry e : manuals){
                out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
            }
            out.println();
        }
    }

    public static class FileReport {
        private final EnumMap<Category, List<Entry>> buckets = new EnumMap<>(Category.class);
        FileReport(){
            for (Category c : Category.values()) buckets.put(c, new ArrayList<>());
        }
        void add(Category c, Entry e){
            buckets.get(c).add(e);
        }

        List<Entry> get(Category c){
            return Collections.unmodifiableList(buckets.get(c));
        }
    }

    public record Entry(String parameter, String message, String newParameter, ReportPreset preset) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Entry other)) return false;
            return Objects.equals(parameter, other.parameter)
                    && Objects.equals(message, other.message)
                    && Objects.equals(newParameter, other.newParameter)
                    && preset == other.preset;
        }

        @Override @NonNull
        public String toString() {
            return "Entry{" +
                    "parameter='" + parameter + '\'' +
                    ", message='" + message + '\'' +
                    ", newParameter='" + newParameter + '\'' +
                    ", preset=" + preset +
                    '}';
        }
    }

    public enum ReportPreset {
        INFO("%s"),
        UNKNOWN_KEY("Encountered unknown key '%s' at path '%s'"),
        INVALID_TYPE("Expected type '%s' for '%s', but found '%s'"),
        MISSING_PARAMETER("'%s' missing required parameter '%s'"),
        IGNORED_KEY("Key '%s' at path '%s' is ignored for migration");

        final String template;

        ReportPreset(String template){
            this.template = template;
        }
        public String format(Object... args) { return String.format(template, args); }
    }

    public void clear(){
        files.clear();
    }

    public Map<String, Map<Category, List<Entry>>> getSnapshot(){
        var snapshot = new LinkedHashMap<String, Map<Category, List<Entry>>>();
        for (var kv : files.entrySet()){
            var map = new EnumMap<Category, List<Entry>>(Category.class);
            for(Category c : Category.values()){
                map.put(c, kv.getValue().get(c));
            }
            snapshot.put(kv.getKey(), map);
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public List<Entry> getEntries(String file, Category c){
        var fr = files.get(file);
        return fr == null ? List.of() : List.copyOf(fr.get(c));
    }

    public int count(Category c){
        return files.values().stream().mapToInt(fr -> fr.get(c).size()).sum();
    }

    public int count(String file, Category c){
        var fr = files.get(file);
        return fr == null ? 0 : fr.get(c).size();
    }

    public static class RoleEntry {
        @NonNull private final String name;
        private final Map<Category, List<Issue>> issues;
        private boolean hasRemoteClusterOrIndex = false;
        private static final String ISSUE_DISPLAY_TEMPLATE = "      - %s\n        -> %s\n";
        private static final String TITLE_DISPLAY_TEMPLATE = "      @|yellow %s (%d)|@\n";

        public RoleEntry(@NonNull String name) {
            this.name = name;
            this.issues = new LinkedHashMap<>();
            issues.put(Category.MANUAL, new LinkedList<>());
            issues.put(Category.WARNING, new LinkedList<>());
        }

        public void hasRemoteClusterOrIndex() { this.hasRemoteClusterOrIndex = true; }
        public void addManualAction(String parameter, String message) { issues.get(Category.MANUAL).add(new Issue(parameter, message)); }
        public void addWarning(String parameter, String message) { issues.get(Category.WARNING).add(new Issue(parameter, message)); }
        public @NonNull String getName() { return this.name; }
        public boolean noIssues() {
            return issues.get(Category.WARNING).isEmpty() && issues.get(Category.MANUAL).isEmpty() && !hasRemoteClusterOrIndex;
        }
        public boolean successful() { return !hasRemoteClusterOrIndex; }
        public Map<Category, List<Issue>> getIssues() { return issues; }

        public void printEntry(PrintStream out) {
            if (hasRemoteClusterOrIndex) {
                out.printf(applyFormating("    @|bold %s:|@\n      - Remote indices and clusters are not supported in Search Guard. The role can not be migrated.\n\n", name));
                return;
            }
            var warnings = issues.get(Category.WARNING);
            var manualActions = issues.get(Category.MANUAL);
            var issueCount = warnings.size() + manualActions.size();
            out.print(applyFormating("    @|bold %s (%d):|@\n", name, issueCount));
            if (!warnings.isEmpty()) {
                out.print(applyFormating(TITLE_DISPLAY_TEMPLATE, "WARNINGS", warnings.size()));
                for (var warning : warnings) {
                    out.printf(ISSUE_DISPLAY_TEMPLATE, warning.parameter, warning.message);
                }
                out.println();
            }
            if (!manualActions.isEmpty()) {
                out.print(applyFormating(TITLE_DISPLAY_TEMPLATE, "MANUAL ACTION REQUIRED", manualActions.size()));
                for (var manualAction : manualActions) {
                    out.printf(ISSUE_DISPLAY_TEMPLATE, manualAction.parameter, manualAction.message);
                }
                out.println();
            }
        }
    }

    public RoleEntries getRoleEntries() { return roleEntries; }

    private static String applyFormating(String format, Object... args) {
        return Ansi.AUTO.string(String.format(format, args));
    }

    public record Issue(String parameter, String message) { }

    public record RoleEntries(List<RoleEntry> successful, List<RoleEntry> withIssues, List<RoleEntry> unsuccessful) { }
}
