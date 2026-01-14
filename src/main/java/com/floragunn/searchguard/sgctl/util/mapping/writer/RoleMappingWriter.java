package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Writes Search Guard role mappings derived from the intermediate representation.
 */
public class RoleMappingWriter implements Document<RoleMappingWriter>{
    private final IntermediateRepresentation ir;
    private final MigrationReport report;
    private final List<SGRoleMapping> rolesMappings;

    static final String FILE_NAME = "sg_roles_mapping.yml";

    public RoleMappingWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.rolesMappings = new ArrayList<>();
        createSGRoleMappings();
    }

    public void createSGRoleMappings() {
        for (var rm : ir.getRoleMappings()) {
            var mappingName = rm.getMappingName();
            var roles = rm.getRoles();
            var templates = rm.getRoleTemplates();

            if (!rm.isEnabled()) {
                report.addWarning(FILE_NAME, mappingName, "Role Mapping Disabled and will hence be ignored");
                continue;
            }

            if (templates != null && !templates.isEmpty()) {
                report.addManualAction(FILE_NAME, mappingName + "->role_templates", "X-Pack role_templates are not automatically migrated.");
                continue;
            }

            for (String roleName : roles) {
                var users = getSGUsers(rm, roleName);
                var backendRoles = getSGBackendRoles(rm, roleName);

                // Hosts and IPs are not available in X-Pack (currently empty lists).
                var hosts = new ArrayList<String>();
                var ips = new ArrayList<String>();

                var sgMapping = new SGRoleMapping(roleName, users, backendRoles, hosts, ips);
                rolesMappings.add(sgMapping);
            }
        }
    }

    private List<String> extractStringValues(Object raw, String path) {
        var result = new ArrayList<String>();

        if (raw == null) {
            return result;
        }

        if (raw instanceof String s) {
            try {
                result.add(LuceneRegexParser.toJavaRegex(s));
            } catch (Exception e) {
                report.addManualAction(FILE_NAME, path, "An error occurred while trying to convert a Lucene regex to a Java regex: " + e.getMessage());
            }
            return result;
        }

        if (raw instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                String itemPath = path + "[" + i + "]";
                if (o instanceof String s) {
                    try {
                        result.add(LuceneRegexParser.toJavaRegex(s));
                    } catch (Exception e) {
                        report.addManualAction(FILE_NAME, itemPath, "An error occurred while trying to convert a Lucene regex to a Java regex: " + e.getMessage());
                    }
                } else {
                    report.addInvalidType(FILE_NAME, itemPath, String.class, o);
                }
            }
            return result;
        }

        report.addInvalidType(FILE_NAME, path, String.class, raw);
        return result;
    }

    private void collectUsernames(RoleMapping.Rules rules, String mappingName, String roleName, String originPath, List<String> usernames) {
        if (rules == null) {
            return;
        }

        var field = rules.getField();
        if (field != null && field.containsKey("username")) {
            usernames.addAll(extractStringValues(field.get("username"), mappingName + "->" + originPath + ".field.username"));
        }

        var anyRules = rules.getAny();
        if (anyRules != null) {
            for (int i = 0; i < anyRules.size(); i++) {
               collectUsernames(anyRules.get(i), mappingName, roleName, originPath + ".any[" + i + "]", usernames);
            }
        }
    }

    private void collectBackendRoles(RoleMapping.Rules rules, String mappingName, String roleName, String originPath,  List<String> backendRoles) {
        if (rules == null) {
            return;
        }

        var field = rules.getField();
        if (field != null) {
            if (field.containsKey("groups")) {
                backendRoles.addAll(extractStringValues(field.get("groups"), mappingName + "->" + originPath + ".field.groups"));
            }

            if (field.containsKey("realm.name")) {
                report.addManualAction(FILE_NAME, mappingName + "->" + roleName + "->" + originPath + ".field.realm.name",
                        "Realm-based role mappings cannot be migrated automatically.");
            }
        }

        var anyRules = rules.getAny();
        if (anyRules != null) {
            for (int i = 0; i < anyRules.size(); i++) {
                collectBackendRoles(anyRules.get(i), mappingName, roleName, originPath + ".any[" + i + "]", backendRoles);
            }
        }

        if (rules.getAll() != null && !rules.getAll().isEmpty()) {
            report.addManualAction(FILE_NAME, mappingName + "->" + roleName + "->rules.all", "XPack rule uses all which cannot be migrated.");
        }
        if (rules.getExcept() != null) {
            report.addManualAction(FILE_NAME, mappingName + "->" + roleName + "->rules.except", "XPack rule uses except which cannot be migrated.");
        }

    }

    public List<String> getSGUsers(RoleMapping rm, String roleName) {
        var result = new ArrayList<String>();
        var rules = rm.getRules();

        if (rules == null) {
            report.addManualAction(FILE_NAME, roleName + "->rules", "Search Guard users for role '" + roleName + "' must be configured manually.");
            return result;
        }

        collectUsernames(rules, rm.getMappingName(), roleName, "rules", result);
        return result;
    }

    public List<String> getSGBackendRoles(RoleMapping rm, String roleName) {
        var result = new ArrayList<String>();
        var rules = rm.getRules();

        // if there are no rules derivable
        if  (rules == null) {
            return result;
        }

        collectBackendRoles(rules, rm.getMappingName(), roleName, "rules", result);
        return result;
    }

    @Override
    public Object toBasicObject() {
        Map<String, SGRoleMapping> merged = new LinkedHashMap<>();
        for (var mapping : rolesMappings) {
            var existing = merged.get(mapping.roleName);

            if (existing == null) {
                merged.put(mapping.roleName, new SGRoleMapping(
                        mapping.roleName,
                        new ArrayList<>(mapping.users),
                        new ArrayList<>(mapping.backendRoles),
                        new ArrayList<>(mapping.hosts),
                        new ArrayList<>(mapping.ips)));
            } else {
                existing.users.addAll(mapping.users);
                existing.backendRoles.addAll(mapping.backendRoles);
                existing.hosts.addAll(mapping.hosts);
                existing.ips.addAll(mapping.ips);
            }
        }
        return merged;
    }

    static class SGRoleMapping implements Document<SGRoleMapping> {
        String roleName;
        List<String> users;
        List<String> backendRoles;
        List<String> hosts;
        List<String> ips;

        SGRoleMapping(String roleName, List<String> users, List<String> backendRoles, List<String> hosts, List<String> ips) {
            this.roleName = roleName;
            this.users = users;
            this.backendRoles = backendRoles;
            this.hosts = hosts;
            this.ips = ips;
        }

        @Override
        public Object toBasicObject() {
            var contents = new LinkedHashMap<String, Object>();

            if (!users.isEmpty()) {
                contents.put("users", users);
            }

            if (!backendRoles.isEmpty()) {
                contents.put("backend_roles", backendRoles);
            }

            if (!hosts.isEmpty()) {
                contents.put("hosts", hosts);
            }

            if (!ips.isEmpty()) {
                contents.put("ips", ips);
            }

            return contents;
        }
    }
}
