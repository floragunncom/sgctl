package com.floragunn.searchguard.sgctl.util.mapping;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MigrationReport {
    public static MigrationReport shared = new MigrationReport();
    private MigrationReport() {}
    private final LinkedHashMap<String, FileReport> files = new LinkedHashMap<>();
    public enum Category {MIGRATED, WARNING, MANUAL}

    /* ----- public API ----- */
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
        System.out.println("---------- Migration Report ----------");
        for (Map.Entry<String, FileReport> fe : files.entrySet()) {
            System.out.println("File: " + fe.getKey() + "\n");
            FileReport fr = fe.getValue();

            printMigrated(fr);
            printWarnings(fr);
            printManuals(fr);
        }  
        System.out.println("---------- End Migration Report ----------");
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
    void printMigrated(FileReport fr){
        List<Entry> migrated = fr.get(Category.MIGRATED);
        if(!migrated.isEmpty()){
            System.out.printf("  MIGRATED (%d)%n  Parameters that have been successfully migrated%n%n", migrated.size());
            for(Entry e : migrated){
                if(e.newParameter != null){
                    System.out.printf("    - %s -> %s%n", e.parameter, e.newParameter);
                } else {
                    System.out.printf("    - %s%n", e.parameter);
                }
            }
            System.out.println();
        } 
    }
    void printWarnings(FileReport fr){
        List<Entry> warnings = fr.get(Category.WARNING);
        if(!warnings.isEmpty()){
            System.out.printf("  %s (%d)%n  Potentially problematic or ambiguous settings. Review them to ensure the migrated configuration behaves as expected%n%n", Category.WARNING.name(), warnings.size());
            printPresets(warnings);
            List<Entry> freeWarnings = new ArrayList<>();
            for (Entry e : warnings) {
                if (e.preset == null) freeWarnings.add(e);
            }
            if (!freeWarnings.isEmpty()) {
                for (Entry e : freeWarnings) {
                    System.out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
                }
                System.out.println();
            }
        }
    }
    void printPresets(List<Entry> entries){
        for (ReportPreset rp : ReportPreset.values()) {
            List<Entry> presetWarnings = new ArrayList<>();
            for (Entry e : entries) {
                if (e.preset == rp) presetWarnings.add(e);
            }
            if (!presetWarnings.isEmpty()) {
                for (Entry e : presetWarnings) {
                    System.out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
                }
                System.out.println();
            }
        }
    }

    void printManuals(FileReport fr){
        List<Entry> manuals = fr.get(Category.MANUAL);
        if(!manuals.isEmpty()){
            System.out.printf("  %s (%d)%n  Parameters that could not be automatically migrated and require manual review or adjustment%n%n", Category.MANUAL.name(), manuals.size());
            for(Entry e : manuals){
                System.out.printf(DISPLAY_TEMPLATE, e.parameter, e.message);
            }
            System.out.println();
        }
    }

    static class FileReport {
        private final EnumMap<Category, List<Entry>> buckets = new EnumMap<>(Category.class);
        FileReport(){
            for (Category c : Category.values()) buckets.put(c, new ArrayList<>());
        }
        void add(Category c, Entry e){
            buckets.get(c).add(e);
        }

        List<Entry> get(Category c){
            return buckets.get(c);
        }
    }

    static class Entry{
        private final String parameter;
        private final String message;
        private final String newParameter;
        private final ReportPreset preset;
        Entry(String parameter, String message, String newParameter, ReportPreset preset){
            this.parameter = parameter;
            this.message = message;
            this.newParameter = newParameter;
            this.preset = preset;
        }
    }

    enum ReportPreset{
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
}


