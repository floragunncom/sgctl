package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.toStringList;

public class RoleMappingConfigReader {
    private final File roleMappingFile;
    private final IntermediateRepresentation ir;
    private final MigrationReport report;

    static final String FILE_NAME = "role_mapping.json";

    public RoleMappingConfigReader(File roleMappingFile, IntermediateRepresentation ir) throws DocumentParseException, IOException {
        this.roleMappingFile = roleMappingFile;
        this.ir = ir;
        this.report = MigrationReport.shared;
        readRoleMappingFile();
    }

    private void readRoleMappingFile() throws DocumentParseException, IOException {
        if (roleMappingFile == null) return;

        var reader = DocReader.json().read(roleMappingFile);

        if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
            report.addInvalidType(FILE_NAME, "origin", LinkedHashMap.class, reader);
            return;
        }

        readRoleMappings(mapReader);
    }

    private void readRoleMappings(LinkedHashMap<?, ?> mapReader) {
        report.addWarning(FILE_NAME, "metadata", "The key 'metadata' is ignored for migration because it has no equivalent in Search Guard");
        for (var entry : mapReader.entrySet()) {
            var rawKey = entry.getKey();

            if (!(rawKey instanceof String key)) {
                report.addInvalidType(FILE_NAME, "origin", String.class, rawKey);
                continue;
            }

            var value = entry.getValue();

            if ((value instanceof LinkedHashMap<?, ?> mapping)) {
                readRoleMapping(mapping, key);
            } else {
                report.addInvalidType(FILE_NAME, key, LinkedHashMap.class, value);
            }
        }
    }

    private void readRoleMapping(LinkedHashMap<?, ?> mapping, String mappingName) {
        var roleMapping = new RoleMapping(mappingName);

        for (var entry : mapping.entrySet()) {
            var rawKey = entry.getKey();
            if (!(rawKey instanceof String key)) {
                report.addInvalidType(FILE_NAME, mappingName, String.class, rawKey);
                continue;
            }
            var value = entry.getValue();
            var path = mappingName + "->" + key;

            switch (key) {
                case "enabled":
                    if (value instanceof Boolean enabled) {
                        roleMapping.setEnabled(enabled);
                    } else {
                        report.addInvalidType(FILE_NAME, path, Boolean.class, value);
                    }
                    break;

                case "roles":
                    var roles = toStringList(value, FILE_NAME, mappingName, key);
                    if (roles != null) {
                        roleMapping.setRoles(roles);
                    }
                    break;

                case "users":
                    var users = toStringList(value, FILE_NAME, mappingName, key);
                    if (users != null) {
                        roleMapping.setUsers(users);
                    }
                    break;

                case "role_templates":
                    if (value instanceof ArrayList<?> templateList) {
                        roleMapping.setRoleTemplates(readRoleTemplates(templateList, mappingName));
                    } else {
                        report.addInvalidType(FILE_NAME, path, ArrayList.class, value);
                    }
                    break;

                case "run_as":
                    var runAs = toStringList(value, FILE_NAME, mappingName, "run_as");
                    if (runAs != null) {
                        roleMapping.setRunAs(runAs);
                    }
                    break;

                case "rules":
                    var rules = readRules(value, mappingName);
                    if (rules != null) {
                        roleMapping.setRules(rules);
                    }
                    break;

                case "metadata":
                    break;

                default:
                    report.addUnknownKey(FILE_NAME, key, path);
                    break;
            }
        }

        var roles = roleMapping.getRoles();
        var templates = roleMapping.getRoleTemplates();
        boolean hasRoles = roles != null && !roles.isEmpty();
        boolean hasTemplates = templates != null && !templates.isEmpty();

        if (!hasRoles && !hasTemplates) {
            report.addMissingParameter(FILE_NAME, "roles / role_templates", mappingName);
        } else if (hasRoles && hasTemplates) {
            report.addWarning(FILE_NAME, mappingName,"Both 'roles' and 'role_templates' are set. Exactly one must be specified.");
        }

        ir.addRoleMapping(roleMapping);
    }

    private RoleMapping.Rules readRules(Object rulesObject, String mappingName) {
        return readRulesInternal(rulesObject, mappingName, "rules");
    }

    private RoleMapping.Rules readRulesInternal(Object rulesObject, String mappingName, String originPath) {
        if (!(rulesObject instanceof LinkedHashMap<?, ?> rulesMap)) {
            report.addInvalidType(FILE_NAME, originPath, Map.class, rulesObject);
            return null;
        }

        var rules = new RoleMapping.Rules();

        for (var entry : rulesMap.entrySet()) {
            var rawKey = entry.getKey();

            if (!(rawKey instanceof String key)) {
                report.addInvalidType(FILE_NAME, originPath, String.class, rawKey);
                continue;
            }
            var value = entry.getValue();
            var childOrigin = originPath + "->" + key;

            switch (key) {
                case "field":
                    if (value instanceof LinkedHashMap<?, ?> fieldMap) {
                        if (fieldMap.keySet().stream().allMatch(String.class::isInstance)) {
                            @SuppressWarnings("unchecked")
                            var safe = (Map<String, Object>) fieldMap;
                            rules.setField(safe);
                        } else {
                            report.addInvalidType(FILE_NAME, childOrigin, Map.class, value);
                        }
                    } else {
                        report.addInvalidType(FILE_NAME, childOrigin, Map.class, value);
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
                    report.addUnknownKey(FILE_NAME, key, childOrigin);
                    break;
            }
        }
        return rules;
    }

    private List<RoleMapping.Rules> readRulesList(Object obj, String mappingName, String path) {
        if (!(obj instanceof List<?> list)) {
            report.addInvalidType(FILE_NAME, path, List.class, obj);
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
                report.addInvalidType(FILE_NAME, path, Map.class, raw);
                continue;
            }

            var roleTemplate = new RoleMapping.RoleTemplate();

            for (var entry : rawMap.entrySet()) {
                var rawKey = entry.getKey();

                if (!(rawKey instanceof String key)) {
                    var keyPath = path + "-><key>";
                    report.addInvalidType(FILE_NAME, keyPath, String.class, rawKey);
                    continue;
                }
                var value = entry.getValue();
                var childPath = path + "->" + key;

                switch (key) {
                    case "format":
                        if (value instanceof String f) {
                            var fmt = RoleMapping.RoleTemplate.Format.fromString(f);
                            if (fmt == null) {
                                report.addInvalidType(FILE_NAME, childPath, String.class, value);
                            } else {
                                roleTemplate.setFormat(fmt);
                            }
                        } else {
                            report.addInvalidType(FILE_NAME, childPath, String.class, value);
                        }
                        break;

                    case "template":
                        if (value instanceof String s) {
                            roleTemplate.setTemplate(s);
                        } else {
                            report.addInvalidType(FILE_NAME, childPath, String.class, value);
                        }
                        break;

                    default:
                        report.addUnknownKey(FILE_NAME, key, childPath);
                        break;
                }
            }

            if (roleTemplate.getTemplate() == null) {
                report.addMissingParameter(FILE_NAME, "template", path);
                continue;
            }

            result.add(roleTemplate);
        }

        return result;
    }
}
