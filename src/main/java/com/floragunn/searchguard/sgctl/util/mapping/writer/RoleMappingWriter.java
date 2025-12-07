package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class RoleMappingWriter implements Document<RoleMappingWriter>{
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGRoleMapping> rolesMappings;

    private static final String FILE_NAME = "sg_roles_mapping.yml";

    public RoleMappingWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.rolesMappings = new ArrayList<>();
        createSGRoleMappings();
        print(DocWriter.yaml().writeAsString(this));
    }

    public void createSGRoleMappings() {
        for (var rm : ir.getRoleMappings()) {
            var mappingName = rm.getMappingName();
            var roles = rm.getRoles();
            var templates = rm.getRoleTemplates();

            if (templates != null) {
                report.addManualAction(FILE_NAME, mappingName + "role_templates", "X-Pack role_templates are not automatically migrated.");
                continue;
            }

            for (String roleName : roles) {
                var users = getSGUsers(rm, roleName);
                var backendeRoles = getSGBackendRoles(rm, roleName);
                var hosts = new ArrayList<String>();
                var ips = new ArrayList<String>();

                var sgMapping = new SGRoleMapping(roleName, users, backendeRoles, hosts, ips);
                rolesMappings.add(sgMapping);
            }
        }
    }

    private SGRoleMapping findOrCreateSGRoleMapping(String roleName) {
        for (var mapping : rolesMappings) {
            if (mapping.roleName.equals(roleName)) {
                return mapping;
            }
        }
        var sgMapping = new SGRoleMapping(roleName, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        rolesMappings.add(sgMapping);
        return sgMapping;
    }

    private List<String> extractStringValues(Object raw, String path) {
        var result = new ArrayList<String>();

        if (raw == null) {
            return result;
        }

        if (raw instanceof String s) {
            result.add(s);
            return result;
        }

        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof String s) {
                    result.add(s);
                } else {
                    report.addInvalidType(FILE_NAME, path, String.class, o);
                }
            }
            return result;
        }

        report.addInvalidType(FILE_NAME, path, String.class, raw);
        return result;
    }

    private void collectUsernames(RoleMapping.Rules rules, String mappingName, String roleName, String originPath, List<String> usernames) {
        var field = rules.getField();
        if (field != null && field.containsKey("username")) {
            var path = mappingName + "->" + originPath + ".field.username";
            usernames.addAll(extractStringValues(field.get("username"), path));
        }

        var anyRules = rules.getAny();
        if (anyRules != null) {
            for (int i = 0; i < anyRules.size(); i++) {
                var child = anyRules.get(i);
                var childPath = originPath + "->any[" + i + "]";
                collectUsernames(child, mappingName, roleName, childPath, usernames);
            }
        }

        var allRules = rules.getAll();
        if (allRules != null) {
            report.addManualAction(FILE_NAME, roleName + "->" + originPath + ".all", "XPack rule uses all which cannot be migrated automatically.");
        }

        if (rules.getExcept() != null) {
            report.addManualAction(FILE_NAME, roleName + "->" + originPath + ".except", "XPack rule uses except which cannot be migrated automatically.");
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
        // TODO: backendRoles migrieren?
        List<String> backendRoles = new ArrayList<>();
        return backendRoles;
    }

    @Override
    public Object toBasicObject() {
        Map<String, SGRoleMapping> contents = new LinkedHashMap<>();
        for (var mapping : rolesMappings) {
            contents.put(mapping.roleName, mapping);
        }
        return contents;
    }

    static class SGRoleMapping implements Document<SGRoleMapping> {
        String roleName;
        List<String> users = new ArrayList<>();
        List<String> backendRoles = new ArrayList<>();
        List<String> hosts = new ArrayList<>();
        List<String> ips = new ArrayList<>();

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

            if (users.isEmpty()) {
                contents.put("users", users);
            }

            if (backendRoles.isEmpty()) {
                contents.put("backend_roles", backendRoles);
            }

            if (hosts.isEmpty()) {
                contents.put("hosts", hosts);
            }

            if (ips.isEmpty()) {
                contents.put("ips", ips);
            }

            return contents;
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
