package com.floragunn.searchguard.sgctl.util.mapping;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.User;
import com.floragunn.searchguard.sgctl.util.mapping.ir.RoleMapping;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
        readRoleMapping();
        ir.getUsers().forEach(user -> print(user.toString()));
        ir.getRoles().forEach(role -> print(role.toString()));
        return ir;
    }

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
        var user = new User(name);
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
                            printErr("Username " + username + " is not equal to the path parameter " + name ); // TODO: Add MigrationReport entry
                            return;
                        }
                    } else {
                        printErr("Invalid type for username:" + value.getClass());// TODO: Add MigrationReport entry
                    }
                    break;
                case "email":
                    if (value instanceof String || value == null) {
                        user.setEmail((String) value);
                    } else {
                        printErr("Invalid value for email: " + value); // TODO: Add MigrationReport entry
                    }
                    break;
                case "full_name":
                    if (value instanceof String || value == null) {
                        user.setFullName((String) value);
                    } else {
                        printErr("Invalid value for full_name: " + value); // TODO: Add MigrationReport entry
                    }
                    break;
                case "metadata":
                    printErr("Metadata is ignored for migration."); // TODO: Add MigrationReport entry
                    break;
                case "roles":
                    var roles = toStringList(value, userFileName, name, key);
                    if (roles == null) { break; }
                    var checkedRoles = new ArrayList<String>();
                    roles.forEach(role -> {
                        if (ir.getRoles().contains(new Role(role))) {
                            checkedRoles.add(role);
                        } else {
                            printErr("Role " + role + " does not exist in role.json."); // TODO: Add MigrationReport entry
                        }
                    });
                    user.setRoles(checkedRoles);
                    break;
                // TODO: Add handling for enabled key
                default:
                    printErr("Unknown key" + key); // TODO: Add MigrationReport entry
                    break;
            }
        }
        ir.addUser(user);
    }

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
                    role.setApplications(readApplications(applicationList));
                } else {
                    printErr("Invalid type for applications: " + value.getClass()); // TODO: Add MigrationReport entry
                }
                break;
            case "cluster":
                try {
                    role.setCluster(toStringList(value));
                } catch (IllegalArgumentException e) {
                    printErr("Invalid type for cluster: " + key.getClass()); // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    printErr("Invalid type for cluster entry: " + value.getClass()); // TODO: Add MigrationReport entry
                }
                break;
            case "indices":
                if (value instanceof ArrayList<?> indices) {
                    role.setIndices(readIndices(indices));
                } else {
                    printErr("Invalid type for indices: " + value.getClass()); // TODO: Add MigrationReport entry
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

    private List<Role.Application> readApplications(ArrayList<?> applicationList) {
        var applications = new ArrayList<Role.Application>();
        for (var rawApplication : applicationList) {
            if (rawApplication instanceof LinkedHashMap<?, ?> applicationMap) {
                var application = readApplication(applicationMap);
                if  (application != null) {
                    applications.add(application);
                } else {
                    printErr("Application was not presented correctly."); // TODO: Add MigrationReport entry
                }
            } else {
                printErr("Invalid type for application: " + rawApplication.getClass()); // TODO: Add MigrationReport entry
            }
        }
        return applications;
    }

    private Role.Application readApplication(LinkedHashMap<?, ?> applicationMap) {
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
                    try {
                        privileges = toStringList(value);
                    } catch (IllegalArgumentException e) {
                        printErr("Invalid type for privileges: " + value.getClass()); // TODO: Add MigrationReport entry
                        return null;
                    } catch (ClassCastException e) {
                        printErr("Invalid type for value: " + e.getMessage()); // TODO: Add MigrationReport entry
                        return null;
                    }
                    break;
                case "resources":
                    try {
                        resources = toStringList(value);
                    } catch (IllegalArgumentException e) {
                        printErr("Invalid type for resources: " + value.getClass()); // TODO: Add MigrationReport entry
                        return null;
                    } catch (ClassCastException e) {
                        printErr("Invalid type for resources: " + e.getMessage()); // TODO: Add MigrationReport entry
                        return null;
                    }
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

    private List<Role.Index> readIndices(ArrayList<?> indexList) {
        var indices = new ArrayList<Role.Index>();
        for (var rawIndex : indexList) {
            if (rawIndex instanceof LinkedHashMap<?, ?> indexMap) {
                var index = readIndex(indexMap);
                if (index == null) {
                    printErr("Index was presented correctly."); // TODO: Add MigrationReport entry
                    continue;
                }
                indices.add(index);
            } else {
                printErr("Invalid type for index: " + rawIndex.getClass()); // TODO: Add MigrationReport entry
            }
        }
        return indices;
    }

    private Role.Index readIndex(LinkedHashMap<?, ?> indexMap) {
        var index = new Role.Index();
        for (var entry : indexMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            // TODO: Add query key
            switch (key) {
            case "field_security":
                if (value instanceof LinkedHashMap<?, ?> fieldSecurity) {
                    index.setFieldSecurity(readFieldSecurity(fieldSecurity));
                    break;
                }
                // TODO: Add MigrationReport entry
                break;
            case "names":
                try {
                    index.setNames(toStringList(value));
                } catch (IllegalArgumentException e) {
                    if (value instanceof String name) {
                        var names = new ArrayList<String>();
                        names.add(name);
                        index.setNames(names);
                        break;
                    }
                    printErr("Invalid type for names: " + value.getClass()); // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    printErr("Invalid type found in names: " + e.getMessage()); // TODO: Add MigrationReport entry
                }
                break;
            case "privileges":
                var privileges = toStringList(value, roleFileName, "", key);
                if (privileges == null) {
                    printErr("Privileges are a required parameter."); // TODO: Add MigrationReport entry
                    break;
                }
                index.setPrivileges(privileges);
                break;
            case "allow_restricted_indices":
                if (value instanceof Boolean allowRestrictedIndices) {
                    index.setAllowRestrictedIndices(allowRestrictedIndices);
                    break;
                }
                printErr("Invalid type for allow_restricted_indices: " + key); // TODO: Add MigrationReport entry
                break;
            default:
                printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
                break;
            }
        }
        if (index.getNames().isEmpty() || index.getPrivileges().isEmpty()) {
            printErr("Index missing required parameter."); // TODO: Add MigrationReport entry
            return null;
        }
        return index;
    }

    private Role.FieldSecurity readFieldSecurity(LinkedHashMap<?, ?> fieldMap) {
        var fieldSecurity = new Role.FieldSecurity();
        for (var entry : fieldMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
            case "except":
                try {
                    fieldSecurity.setExcept(toStringList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                }
                break;
            case "grant":
                try {
                    fieldSecurity.setGrant(toStringList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                }
                break;
            default:
                // TODO: Add MigrationReport entry
            }
        }
        return fieldSecurity;
    }

    private void readRoleMapping() {
        if (roleMappingFile == null) return;
        try {
            var reader = DocReader.json().read(roleMappingFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }

            for (var entry : mapReader.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                    continue;
                }
                var value = entry.getValue();

                if ((value instanceof LinkedHashMap<?, ?> mapping)) {
                    readSingleRoleMapping(mapping, key);
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Unexpected value for key " + key);
                }
            }

        } catch (Exception e) {
            // TODO: Add proper error handling
            printErr(e.getMessage());
        }
    }

    private void readSingleRoleMapping(LinkedHashMap<?, ?> mapping, String mappingName) {
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

                case "users":
                    try {
                        roleMapping.setUsers(toStringList(value));
                    } catch (IllegalArgumentException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for users: " + value.getClass());
                    } catch (ClassCastException e) {
                        printErr("Invalid entry in 'users' for role mapping '" + mappingName + "': " + e.getMessage());
                    }
                    break;
                case "rules":
                    // TODO: Implement readRules()
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

    public void readRules(Object rulesObject, String mappingName) {
        if (!(rulesObject instanceof LinkedHashMap<?, ?> rulesMap)) {
            // TODO: Add MigrationReport entry
            printErr("Invalid type for rules in role mapping '" + mappingName + "': " + rulesObject.getClass());
            return;
        }

        var fieldObj = rulesMap.get("field");
        if (fieldObj instanceof LinkedHashMap<?, ?> fieldMap) {
            for (var entry : fieldMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                    continue;
                }
                var value = entry.getValue();
                // TODO Add field rule to RoleMapping
            }
        }
    }

    private static String toString(Object obj, String originFile, String parameterOrigin, String key) {
        if (obj instanceof String value) {
            return value;
        }
        printErr("Issue at file: " + originFile + " and parameter: " + parameterOrigin + ". Invalid type " + obj.getClass() + " for key " + key); // TODO: Add MigrationReport entry
        return null;
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
}
