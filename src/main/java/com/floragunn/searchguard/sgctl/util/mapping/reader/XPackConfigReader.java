package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File roleMappingFile;
    IntermediateRepresentation ir;
    MigrationReport report;

    static final String roleMappingFileName = "role_mapping.json";

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile, MigrationReport report) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
        this.ir = new IntermediateRepresentation();
        this.report = report;
    }

    public IntermediateRepresentation generateIR() {
        new RoleConfigReader(roleFile, ir, report);
        new UserConfigReader(userFile, ir, report);

        readRoleMappingFile();
        ir.getUsers().forEach(user -> print(user.toString()));
        ir.getRoles().forEach(role -> print(role.toString()));

        return ir;
    }

    //region Role Mapping
    private void readRoleMappingFile() {
        if (roleMappingFile == null) return;
        try {
            var reader = DocReader.json().read(roleMappingFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }

            readRoleMappings(mapReader);

        } catch (DocumentParseException e) {
            printErr("Error while parsing file."); // TODO: Add MigrationReport entry
        } catch (FileNotFoundException e) {
            printErr("File not found."); // TODO: Add MigrationReport entry
        } catch (IOException e) {
            printErr("Unexpected Error while accessing file."); // TODO: Add MigrationReport entry
        }
    }

    private void readRoleMappings(LinkedHashMap<?, ?> mapReader) {
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            if ((value instanceof LinkedHashMap<?, ?> mapping)) {
                readRoleMapping(mapping, key);
            } else {
                // TODO: Add MigrationReport entry
                printErr("Unexpected value for key " + key);
            }
        }
    }

    private void readRoleMapping(LinkedHashMap<?, ?> mapping, String mappingName) {
        var roleMapping = new RoleMapping(mappingName);

        for (var entry : mapping.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "enabled":
                    if (value instanceof Boolean enabled) {
                        roleMapping.setEnabled(enabled);
                    } else {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for enabled: " + value.getClass());
                    }
                    break;

                case "roles":
                    try {
                        roleMapping.setRoles(toStringList(value));
                    } catch (IllegalArgumentException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for roles: " + value.getClass());
                    } catch (ClassCastException e) {
                        printErr("Invalid entry in 'roles' for role mapping '" + mappingName + "': " + e.getMessage());
                    }
                    break;

                case "role_templates":
                    if (value instanceof ArrayList<?> templateList) {
                        roleMapping.setRoleTemplates(readRoleTemplates(templateList, mappingName));
                    } else {
                        // TODO: MigrationReport entry
                        printErr("Invalid type for role_templates: " + value.getClass());
                    }
                    break;

                case "run_as":
                    var runAs = toStringList(value, roleMappingFileName, mappingName, "run_as");
                    if (runAs != null) {
                        roleMapping.setRunAs(runAs);
                    }
                    break;

                case "rules":
                    roleMapping.setRules(readRules(value, mappingName));
                    break;

                case "metadata":
                    // TODO: Implement readMetadata() if necessary
                    break;

                default:
                    // TODO: Add MigrationReport entry
                    printErr("Unknown key: " + key);
                    break;
            }
        }

        ir.addRoleMapping(roleMapping);
    }

    private RoleMapping.Rules readRules(Object rulesObject, String mappingName) {
        if (!(rulesObject instanceof LinkedHashMap<?, ?> rulesMap)) {
            // TODO: Add MigrationReport entry
            printErr("Invalid type for rules in role mapping '" + mappingName + "': " + rulesObject.getClass());
            return null;
        }

        var rules = new RoleMapping.Rules();

        var fieldObj = rulesMap.get("field");
        if (fieldObj instanceof LinkedHashMap<?, ?> fieldMap) {
            for (var entry : fieldMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                    continue;
                }
                var value = entry.getValue();
                // TODO: Add field rule to RoleMapping
            }
        }
        return rules;
    }

    private List<RoleMapping.RoleTemplate> readRoleTemplates(ArrayList<?> templateList, String mappingName) {
        var templates = new ArrayList<RoleMapping.RoleTemplate>();

        // TODO: Implement readRoleTemplates
        return templates;
    }
    //endregion

    //region Helper Functions
    <R> R toType(Object value, Class<R> expectedType, String fileName, String path) {
        if (expectedType.isInstance(value)) {
            return expectedType.cast(value);
        }
        report.addInvalidType(fileName, path, expectedType.getTypeName(), value.getClass().getTypeName());
        return null;
    }

    static <R> List<R> readList(List<?> rawList, Function<LinkedHashMap<?, ?>, R> reader) {
        var result = new ArrayList<R>();
        for (var element : rawList) {
            if (element instanceof LinkedHashMap<?, ?> rawMap) {
                var value = reader.apply(rawMap);
                if (value == null) {
                    printErr("Item was not presented correctly.");
                    continue;
                }
                result.add(value);
            } else {
                printErr("Invalid type for item: " + element.getClass());
            }
        }
        return result;
    }

    static List<String> toStringList(Object obj, String originFile, String parameterOrigin, String key) {
        try {
            return toStringList(obj);
        } catch (IllegalArgumentException e) {
            printErr("Issue at file: " + originFile + " and parameter: " + parameterOrigin + ". Invalid type " + obj.getClass() + " for key " + key); // TODO: Add MigrationReport entry
        } catch (ClassCastException e) {
            printErr("Issue at file: " + originFile + " and parameter: " + parameterOrigin + ". Expected 'String' for item in list for key" + key + " but got " + e.getMessage()); // TODO: Add MigrationReport entry
        }
        return null;
    }

    private static List<String> toStringList(Object obj) throws IllegalArgumentException, ClassCastException {
        if (!(obj instanceof List<?> list)) {
            throw new IllegalArgumentException("Object is not a List");
        }

        ArrayList<String> result = new ArrayList<>();

        for (Object element : list) {
            if (!(element instanceof String)) {
                throw new ClassCastException("" + element.getClass());
            }
            result.add((String) element);
        }

        return result;
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
    //endregion
}
