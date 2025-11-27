package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import org.jspecify.annotations.NonNull;

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

    static final String roleFileName = "role.json";
    static final String userFileName = "user.json";
    static final String roleMappingFileName = "role_mapping.json";

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
        this.ir = new IntermediateRepresentation();
    }

    public IntermediateRepresentation generateIR() {
        readRoleFile();
        readUserFile();
        readRoleMappingFile();
        ir.getUsers().forEach(user -> print(user.toString()));
        ir.getRoles().forEach(role -> print(role.toString()));

        return ir;
    }

    //region User
    private void readUserFile() {
        if (userFile == null) return;
        try {
            var reader = DocReader.json().read(userFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }
            readUsers(mapReader);
        } catch (DocumentParseException e) {
            printErr("Error while parsing file."); // TODO: Add MigrationReport entry
        } catch (FileNotFoundException e) {
            printErr("File not found."); // TODO: Add MigrationReport entry
        } catch (IOException e) {
            printErr("Unexpected Error while accessing file."); // TODO: Add MigrationReport entry
        }
    }

    private void readUsers(LinkedHashMap<?, ?> mapReader) {
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            if (value instanceof LinkedHashMap<?, ?> user) {
                readUser(user, key);
            } else {
                printErr("Unexpected value for key " + key); // TODO: Add MigrationReport entry
            }
        }
    }

    private void readUser(LinkedHashMap<?, ?> userMap, String name) {
        List<String> roles = null;
        Boolean enabled = null;
        String fullName = null;
        String email = null;
        String profileUID = null;
        LinkedHashMap<String, Object> attributes = null;

        for (var entry : userMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
                case "username":
                    if (value instanceof String username) {
                        // TODO: Check thoroughly if this is an actual requirement
                        if (!name.equals(username)) {
                            printErr("Username " + username + " is not equal to the path parameter " + name); // TODO: Add MigrationReport entry
                            return;
                        }
                    } else {
                        printErr("Invalid type for username:" + value.getClass());// TODO: Add MigrationReport entry
                    }
                    break;
                case "email":
                    if (value instanceof String || value == null) {
                        email = (String)  value;
                    } else {
                        printErr("Invalid value for email: " + value); // TODO: Add MigrationReport entry
                    }
                    break;
                case "full_name":
                    if (value instanceof String || value == null) {
                        fullName = (String) value;
                    } else {
                        printErr("Invalid type for full_name: " + value); // TODO: Add MigrationReport entry
                    }
                    break;
                case "metadata":
                    if (value instanceof LinkedHashMap<?, ?> metadata) {
                        if (metadata.keySet().stream().allMatch(metadataKey -> metadataKey instanceof String)) {
                            @SuppressWarnings("unchecked") // Cast is logically checked to always be possible
                            var safeMap = (LinkedHashMap<String, Object>) metadata;
                            attributes = safeMap;
                        } else {
                            printErr("Invalid type for metadata: " + value); // TODO: Add MigrationReport entry
                        }
                    } else {
                        // TODO: Add MigrationReport entry
                    }
                    break;
                case "roles":
                    var uncheckedRoles = toStringList(value, userFileName, name, key);
                    if (uncheckedRoles == null) {
                        // TODO: Add MigrationReport entry
                        break;
                    }
                    var checkedRoles = new ArrayList<String>();
                    uncheckedRoles.forEach(role -> {
                        if (ir.getRoles().contains(new Role(role))) {
                            checkedRoles.add(role);
                        } else {
                            printErr("Role " + role + " does not exist in role.json."); // TODO: Add MigrationReport entry
                        }
                    });
                    roles = checkedRoles;
                    break;
                case "enabled":
                    if (value instanceof Boolean) {
                        enabled = (Boolean) value;
                        break;
                    }
                    // TODO: Add MigrationReport entry
                    break;
                case "profile_uid":
                    if (value instanceof String uid) {
                        profileUID = uid;
                        break;
                    }
                    // TODO: Add MigrationReport entry
                    break;
                default:
                    printErr("Unknown key " + key); // TODO: Add MigrationReport entry
                    break;
            }
        }

        if (enabled == null) {
            printErr("Missing required parameter 'enabled'"); // TODO: Add MigrationReport entry
            return;
        }
        if (roles == null) {
            printErr("Missing required parameter 'roles'"); // TODO: Add MigrationReport entry
            return;
        }
        if (attributes == null) {
            printErr("Missing required parameter 'attributes'"); // TODO: Add MigrationReport entry
            return;
        }

        var user = new User(name, roles, fullName, email, enabled, profileUID, attributes);
        ir.addUser(user);
    }
    //endregion

    //region Roles
    private void readRoleFile() {
        if (roleFile == null) return;
        try {
            var reader = DocReader.json().read(roleFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }
            readRoles(mapReader);
        } catch (DocumentParseException e) {
            printErr("Error while parsing file."); // TODO: Add MigrationReport entry
        } catch (FileNotFoundException e) {
            printErr("File not found."); // TODO: Add MigrationReport entry
        } catch (IOException e) {
            printErr("Unexpected Error while accessing file."); // TODO: Add MigrationReport entry
        }
    }

    private void readRoles(LinkedHashMap<?, ?> mapReader) {
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            if (value instanceof LinkedHashMap<?, ?> role) {
                readRole(role, key);
            } else {
                printErr("Invalid value for role: " + value); // TODO: Add MigrationReport entry
            }
        }
    }

    private void readRole(LinkedHashMap<?, ?> roleMap, @NonNull String roleName) {
        var role = new Role(roleName);
        for (var entry : roleMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "applications":
                    if (value instanceof ArrayList<?> applicationList) {
                        role.setApplications(readList(applicationList, map->readApplication(map, roleName+"->applications")));
                    } else {
                        printErr("Invalid type for applications: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "cluster":
                    role.setCluster(toStringList(value, roleFileName, roleName, key));
                    break;
                case "remote_cluster":
                    if (value instanceof ArrayList<?> remoteClusterList) {
                        role.setRemoteClusters(readList(remoteClusterList, map->readRemoteCluster(map, roleName+"->remoteClusters")));
                    }
                    break;
                case "indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setIndices(readList(indices, map -> readIndex(map, false, roleName + "->indices", Role.Index.class)));
                    } else {
                        printErr("Invalid type for indices: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "remote_indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setRemoteIndices(readList(indices, map -> readRemoteIndex(map, roleName + "->remote_indices")));
                    } else {
                        printErr("Invalid type for remote_indices: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "metadata":
                    printErr("Metadata is ignored for migration."); // TODO: Add MigrationReport entry
                    break;
                case "transient_metadata":
                    // TODO: Add support for transient metadata interpretation
                    break;
                case "run_as":
                    role.setRunAs(toStringList(value, roleFileName, roleName, "runAs"));
                    break;
                case "description":
                    if (value instanceof String description) {
                        role.setDescription(description);
                    } else {
                        printErr("Invalid type for description: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
            }
        }

        ir.addRole(role);
    }

    private Role.RemoteCluster readRemoteCluster(LinkedHashMap<?, ?> map, String origin) {
        var remoteCluster = new Role.RemoteCluster();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
                case "clusters":
                    remoteCluster.setClusters(toStringList(value, roleFileName, origin, key));
                    break;
                case "privileges":
                    remoteCluster.setPrivileges(toStringList(value, roleFileName, origin, key));
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
            }
        }

        return remoteCluster;
    }

    private Role.Application readApplication(LinkedHashMap<?, ?> applicationMap, String origin) {
        String name = null;
        List<String> privileges = null;
        List<String> resources = null;
        for (var entry : applicationMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "application":
                    if (value instanceof String applicationName) {
                        name = applicationName;
                    } else {
                        printErr("Invalid value type for application: " + value.getClass()); // TODO: Add MigrationReport entry
                        return null;
                    }
                    break;
                case "privileges":
                    privileges = toStringList(value, roleFileName, origin, key);
                    break;
                case "resources":
                    resources = toStringList(value, roleFileName, origin, key);
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
                    break;
            }
        }
        if (name == null || privileges == null || resources == null) {
            printErr("Application missing required parameter."); // TODO: Add MigrationReport entry
            return null;
        }
        return new Role.Application(name, privileges, resources);
    }

    private Role.RemoteIndex readRemoteIndex(LinkedHashMap<?, ?> indexMap, String origin) {
        return readIndex(indexMap, true, origin, Role.RemoteIndex.class);
    }

    private <T extends Role.Index> T readIndex(LinkedHashMap<?, ?> indexMap, boolean isRemote, String origin, Class<T> type) {
        List<String> cluster = null;
        List<String> names = null;
        List<String> privileges = null;
        Role.FieldSecurity fieldSecurity = null;
        String query = null;
        Boolean allowRestricted = null;
        for (var entry : indexMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            // TODO: Add query key
            switch (key) {
                case "clusters":
                    if (!isRemote) {
                        printErr("Found unexpected key clusters. In non remote index."); // TODO: Add MigrationReport entry
                        break;
                    }
                    if (value instanceof String clusterName) {
                        cluster = new ArrayList<>();
                        cluster.add(clusterName);
                        break;
                    }
                    cluster = toStringList(value, roleFileName, origin, key);
                    break;
                case "field_security":
                    if (value instanceof LinkedHashMap<?, ?> fieldSecurityMap) {
                        fieldSecurity = readFieldSecurity(fieldSecurityMap, origin);
                        break;
                    }
                    // TODO: Add MigrationReport entry
                    break;
                case "names":
                    if (value instanceof String name) {
                        names = new ArrayList<>();
                        names.add(name);
                        break;
                    }
                    names = toStringList(value, roleFileName, origin, key);
                    break;
                case "privileges":
                    privileges = toStringList(value, roleFileName, origin, key);
                    break;
                case "query":
                    if (value instanceof String) {
                        query = (String) value;
                        break;
                    }
                    printErr("Unsupported format for query."); // TODO: possibly add support for other formats see: https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-security-put-role#operation-security-put-role-body-application-json-indices-query
                    break;
                case "allow_restricted_indices":
                    if (value instanceof Boolean allowRestrictedIndices) {
                        allowRestricted = allowRestrictedIndices;
                        break;
                    }
                    printErr("Invalid type for allow_restricted_indices: " + key); // TODO: Add MigrationReport entry
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
                    break;
            }
        }
        if (names == null) {
            printErr("Index missing required parameter 'names'"); // TODO: Add MigrationReport entry
            return null;
        }

        if (privileges == null) {
            printErr("Index missing required parameter 'names'"); // TODO: Add MigrationReport entry
            return null;
        }
        if (isRemote) {
            if (cluster == null) {
                printErr("Index missing required parameter 'clusters'"); // TODO: Add MigrationReport entry
                return null;
            }
            return type.cast(new Role.RemoteIndex(cluster, names, privileges, fieldSecurity, query, allowRestricted));
        }
        return type.cast(new Role.Index(names, privileges, fieldSecurity, query, allowRestricted));
    }

    private Role.FieldSecurity readFieldSecurity(LinkedHashMap<?, ?> fieldMap, String origin) {
        origin = origin + "->field_security";
        var fieldSecurity = new Role.FieldSecurity();
        for (var entry : fieldMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
                case "except":
                    fieldSecurity.setExcept(toStringList(value, roleFileName, origin, key));
                    break;
                case "grant":
                    fieldSecurity.setGrant(toStringList(value, roleFileName, origin, key));
                    break;
                default:
                    // TODO: Add MigrationReport entry
                    break;
            }
        }
        return fieldSecurity;
    }
    //endregion

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

        var roles = roleMapping.getRoles();
        var templates = roleMapping.getRoleTemplates();

        boolean hasRoles = roles != null && !roles.isEmpty();
        boolean hasTemplates = templates != null && !templates.isEmpty();

        if (!hasRoles && !hasTemplates) {
            printErr("Role mapping '" + mappingName + "' must define either 'roles' or 'role_templates'.");
            //  TODO: migrationReport.addMissingParameter
        } else if (hasRoles && hasTemplates) {
            printErr("Role mapping '" + mappingName + "' defines both 'roles' and 'role_templates'. Only one is allowed.");
            //  TODO: migrationReport.addInvalidType
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
    private <R> List<R> readList(List<?> rawList, Function<LinkedHashMap<?, ?>, R> reader) {
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

    private static List<String> toStringList(Object obj, String originFile, String parameterOrigin, String key) {
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
