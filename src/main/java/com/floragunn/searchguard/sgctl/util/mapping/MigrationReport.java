package com.floragunn.searchguard.sgctl.util.mapping;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

/*
    Erste Idde den Migration Report zu strukturieren.
    Von außen zu benutzen als addMigrated/Unmapped/Warning/ManualAction(String filename, String parameter, String message)
    Intern strukturiert als:
    file:
        Category:
            Parameter: message
        Category:
        .
        .
        .
    file:
        .
*/
public class MigrationReport {
    private final LinkedHashMap<String, FileReport> files = new LinkedHashMap<>();
    public enum Category {MIGRATED, UNMAPPED, WARNING, MANUAL}

    public void addMigrated(String file, String parameter, String message){
        file(file).add(Category.MIGRATED, new Entry(parameter, message));
    }

    public void addUnmapped(String file, String parameter, String message){
        file(file).add(Category.UNMAPPED, new Entry(parameter, message));
    }

    public void addWarning(String file, String parameter, String message){
        file(file).add(Category.WARNING, new Entry(parameter, message));
    }

    public void addManualAction(String file, String parameter, String message){
        file(file).add(Category.MANUAL, new Entry(parameter, message));
    }

    private FileReport file(String file){
        return files.computeIfAbsent(file, k -> new FileReport());
    }

    public void printReport(){}

    static class FileReport {
        private final EnumMap<Category, List<Entry>> buckets = new EnumMap<>(Category.class);
        FileReport(){
            for (Category c : Category.values()) buckets.put(c, new ArrayList<>());
        }
        void add(Category c, Entry e){}
    }
    static class Entry{
        private final String parameter;
        private final String message;
        Entry(String parameter, String message){
            this.parameter = parameter;
            this.message = message;
        }
    }
}
