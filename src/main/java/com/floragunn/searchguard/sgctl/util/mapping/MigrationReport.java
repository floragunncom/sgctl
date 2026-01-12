package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.writer.RoleConfigWriter;
import org.jspecify.annotations.NonNull;

import java.io.PrintStream;
import java.util.*;


/**
 * Collects migration results and renders a readable report grouped by category.
 */
public class MigrationReport {
    public static MigrationReport shared = new MigrationReport();
    private MigrationReport() {}
    private final LinkedHashMap<String, FileReport> files = new LinkedHashMap<>();
    private final List<RoleEntry> roleEntries = new LinkedList<>();
    public enum Category {MIGRATED, WARNING, MANUAL}

    /* ----- public API ----- */
    public void addRoleEntry(RoleEntry entry) {
        roleEntries.add(entry);
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
        out.println("---------- Migration Report ----------");
        for (Map.Entry<String, FileReport> fe : files.entrySet()) {
            out.println("File: " + fe.getKey() + "\n");
            FileReport fr = fe.getValue();

            printMigrated(fr, out);
            printWarnings(fr, out);
            printManuals(fr, out);
        }
        out.println("File: " + RoleConfigWriter.FILE_NAME + "\n");
        for (var entry : roleEntries) {
            entry.printEntry(out);
        }
        out.println("---------- End Migration Report ----------");
    }

    /* ---------- internals ---------- */
    private static final String DISPLAY_TEMPLATE = "    - %s%n      -> %s%n";
    
    private FileReport file(String file){
        return files.computeIfAbsent(file, k -> new FileReport());
    }

    private void addPreset(ReportPreset rp,Category category, String file, String parameter, Object... args){
        String msg = rp.format(args);
        file(file).add(category, new Entry(parameter, msg, null, rp));
    }
    void printMigrated(FileReport fr, PrintStream out){
        List<Entry> migrated = fr.get(Category.MIGRATED);
        if(!migrated.isEmpty()){
            out.printf("  MIGRATED (%d)%n  Parameters that have been successfully migrated%n%n", migrated.size());
            for(Entry e : migrated){
                if(e.newParameter != null){
                    out.printf("    - %s -> %s%n", e.parameter, e.newParameter);
                } else {
                    out.printf("    - %s%n", e.parameter);
                }
            }
            out.println();
        }
    }
    void printWarnings(FileReport fr, PrintStream out){
        List<Entry> warnings = fr.get(Category.WARNING);
        if(!warnings.isEmpty()){
            out.printf("  %s (%d)%n  Potentially problematic or ambiguous settings. Review them to ensure the migrated configuration behaves as expected%n%n", Category.WARNING.name(), warnings.size());
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
            out.printf("  %s (%d)%n  Parameters that could not be automatically migrated and require manual review or adjustment%n%n", Category.MANUAL.name(), manuals.size());
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

    public static class Entry{
        private final String parameter;
        private final String message;
        private final String newParameter;
        private final ReportPreset preset;
        public Entry(String parameter, String message, String newParameter, ReportPreset preset){
            this.parameter = parameter;
            this.message = message;
            this.newParameter = newParameter;
            this.preset = preset;
        }
        public String getParameter() { return parameter; }
        public String getMessage() { return message; }
        public String getNewParameter() { return newParameter; }
        public ReportPreset getPreset() { return preset; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Entry)) return false;
            Entry other = (Entry) obj;
            return Objects.equals(parameter, other.parameter)
                && Objects.equals(message, other.message)
                && Objects.equals(newParameter, other.newParameter)
                && preset == other.preset;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(parameter, message, newParameter, preset);
        }
    
        @Override
        public String toString() {
            return "Entry{" +
                "parameter='" + parameter + '\'' +
                ", message='" + message + '\'' +
                ", newParameter='" + newParameter + '\'' +
                ", preset=" + preset +
                '}';
        }
    }

    public enum ReportPreset{
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
        private boolean hasRunAs = false;
        private boolean hasApplications = false;

        public RoleEntry(@NonNull String name) {
            this.name = name;
            this.issues = new LinkedHashMap<>();
            issues.put(Category.MANUAL, new LinkedList<>());
            issues.put(Category.WARNING, new LinkedList<>());
        }

        public void hasRemoteClusterOrIndex() { this.hasRemoteClusterOrIndex = true; }
        public void hasRunAs() { this.hasRunAs = true; }
        public void hasApplications() {this.hasApplications = true; }
        public void addManualAction(String parameter, String message) { issues.get(Category.MANUAL).add(new Issue(parameter, message)); }
        public void addWarning(String parameter, String message) { issues.get(Category.WARNING).add(new Issue(parameter, message)); }

        public void printEntry(PrintStream out) {
            out.println("\t" + name + ":");
            var warnings = issues.get(Category.WARNING);
            var manualActions = issues.get(Category.MANUAL);
            if (warnings.isEmpty() && manualActions.isEmpty() && !hasApplications && !hasRemoteClusterOrIndex && !hasRunAs) {
                out.println("\t\tSuccessfully migrated the role with no issues.\n");
                return;
            }
            if (hasRemoteClusterOrIndex) {
                out.println("\t\tRemote indices and clusters are not supported in Search Guard. The role can not be migrated.\n");
                return;
            }
            var issueCount = warnings.size() + manualActions.size() + (hasRunAs ? 1 : 0) + (hasApplications ? 1 : 0);
            out.println("\t\tSuccessfully Migrated with " + issueCount + (issueCount == 1 ? " issue." : " issues.") + "\n");
            if (hasRunAs) out.println("\t\tThere is no equivalent to 'run as' in Search Guard. Run as is therefor ignored.\n");
            if (hasApplications) out.println("\t\tThere is no equivalent to 'application' in Search Guard. All its entries are therefor ignored.\n");
            if (!warnings.isEmpty()) {
                out.println("\t\tWarnings(" + warnings.size() + "):");
                for (var warning : warnings) {
                    out.println("\t\t" + warning.parameter);
                    out.println("\t\t\t" + warning.message);
                }
                out.println();
            }
            if (!manualActions.isEmpty()) {
                out.println("\t\tManual Actions (" + manualActions.size() + "):");
                for (var manualAction : manualActions) {
                    out.println("\t\t" + manualAction.parameter);
                    out.println("\t\t\t" + manualAction.message);
                }
                out.println();
            }
        }
    }

    public record Issue(String parameter, String message) { }
}
