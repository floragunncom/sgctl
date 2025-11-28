package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.readList;
import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.toStringList;

public class RoleMappingConfigReader {
    File roleMappingFile;
    IntermediateRepresentation ir;
    MigrationReport report;

    static final String FILE_NAME = "role_mapping.json";

    public RoleMappingConfigReader(File roleMappingFile, IntermediateRepresentation ir, MigrationReport report) {
        this.roleMappingFile = roleMappingFile;
        this.ir = ir;
        this.report = report;
        readRoleMappingFile();
    }

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
                        roleMapping.setRoles(toStringList(value, FILE_NAME, mappingName, key));
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
                    var runAs = toStringList(value, FILE_NAME, mappingName, "run_as");
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

        var roles = roleMapping.getRoles();
        var templates = roleMapping.getRoleTemplates();
        boolean hasRoles = roles != null && !roles.isEmpty();
        boolean hasTemplates = templates != null && !templates.isEmpty();

        if (!hasRoles && !hasTemplates) {
            // TODO: Add MigrationReport entry (Missing parameter)
        } else if (hasRoles && hasTemplates) {
            // TODO: Add MigrationReport entry (Only one Parameter)
        }

        ir.addRoleMapping(roleMapping);
    }

    private RoleMapping.Rules readRules(Object rulesObject, String mappingName) {
        return readRulesInternal(rulesObject, mappingName, "rules");
    }

    private RoleMapping.Rules readRulesInternal(Object rulesObject, String mappingName, String originPath) {
        if (!(rulesObject instanceof LinkedHashMap<?, ?> rulesMap)) {
            printErr("Invalid type " + rulesObject.getClass() + " for key.");
            // TODO: Add MigrationReport entry
            return null;
        }

        var rules = new RoleMapping.Rules();

        for (var entry : rulesMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid key type in " + originPath + " of " + mappingName
                        + ": " + entry.getKey().getClass());
                continue;
            }
            var value = entry.getValue();
            var childOrigin = originPath + "->" + key;

            switch (key) {
                case "field":
                    if (value instanceof LinkedHashMap<?, ?> fieldMap) {
                        if (fieldMap.keySet().stream().allMatch(k -> k instanceof String)) {
                            var safe = (Map<String, Object>) fieldMap;
                            rules.setField(safe);
                        } else {
                            // TODO: Add MigrationReport entry
                            printErr("Map with no String key");
                        }
                    } else {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid key type in " + originPath + " of " + mappingName);
                    }
                    break;

                case "any":
                    rules.setAny(readRulesList(value, mappingName, childOrigin));
                    break;

                case "all":
                    rules.setAll(readRulesList(value, mappingName, childOrigin));
                    break;

                case "except":
                    rules.setExcept(readRulesInternal(value, mappingName, childOrigin));
                    break;

                default:
                    printErr("Unknown key in " + originPath + " for mapping " + mappingName + ": " + key);
                    break;
            }
        }
        return rules;
    }

    private List<RoleMapping.Rules> readRulesList(Object obj, String mappingName, String path) {
        if (!(obj instanceof List<?> list)) {
            MigrationReport.shared.addInvalidType(FILE_NAME, path, List.class, obj.getClass().getTypeName());
            return null;
        }

        var result = new ArrayList<RoleMapping.Rules>();
        for (int i = 0; i < list.size(); i++) {
            var element = list.get(i);
            var elementPath = path + "[" + i + "]";

            var childRules = readRulesInternal(element, mappingName, elementPath);
            if (childRules != null) {
                result.add(childRules);
            }
        }
        return result;
    }

    private List<RoleMapping.RoleTemplate> readRoleTemplates(List<?> templateList, String mappingName) {
        var result = new ArrayList<RoleMapping.RoleTemplate>();

        for (int i = 0; i < templateList.size(); i++) {
            var raw = templateList.get(i);
            var path = mappingName + "->role_template[" + i + "]";

            if (!(raw instanceof LinkedHashMap<?, ?> rawMap)) {
                // TODO: Add MigrationReport entry
                printErr("Invalid type " + raw.getClass() + " for key.");
                continue;
            }

            var roleTemplate = new RoleMapping.RoleTemplate();

            for (var entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type " + entry.getKey().getClass() + " for key.");
                    continue;
                }
                var value = entry.getValue();
                var childPath = path + "->" + key;

                switch (key) {
                    case "format":
                        if (value instanceof String f) {
                            var fmt = RoleMapping.RoleTemplate.Format.fromString(f);
                            if (fmt == null) {
                                // TODO: MigrationReport entry
                                printErr("Unknown format '" + f + "' in " + childPath);
                            } else {
                                roleTemplate.setFormat(fmt);
                            }
                        } else {
                            // TODO: MigrationReport entry
                            printErr("Invalid type " + value.getClass() + " for " + childPath + ". Expected String.");
                        }
                        break;

                    case "template":
                        if (value instanceof String s) {
                            roleTemplate.setTemplate(s);
                        } else {
                            // TODO: Add MigrationReport entry
                            printErr("Invalid type " + value.getClass() + " for " + childPath + ". Expected String.");
                        }
                        break;

                    default:
                        // TODO: Add MigrationReport entry
                        printErr("Unknown key in " + mappingName + ": " + key);
                }
            }

            result.add(roleTemplate);
        }

        return result;
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
