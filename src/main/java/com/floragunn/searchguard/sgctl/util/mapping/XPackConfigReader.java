package com.floragunn.searchguard.sgctl.util.mapping;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.User;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File roleMapping;
    IntermediateRepresentation ir;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMapping) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMapping = roleMapping;
        this.ir = new IntermediateRepresentation();
    }

    public IntermediateRepresentation generateIR() {
//        readUser();
        readRole();
//        readRoleMapping();
        ir.getRoles().forEach(role -> {
            print(role.toString());
        });
        return ir;
    }

    private void readUser() {
        try {
            var reader = DocReader.json().read(userFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }
            for (var entry : mapReader.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (!(key instanceof String)) {
                    // TODO: Add MigrationReport entry
                    continue;
                }

                if (value instanceof LinkedHashMap<?, ?> user) {
                    readSingleUser(user);
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

    private void readSingleUser(LinkedHashMap<?, ?> user) {
        String username = null;
        String email = null;
        String fullName = null;
        for (var entry : user.entrySet()) {
            var key = (String) entry.getKey();
            var value = entry.getValue();
            switch (key) {
                case "username":
                    if (value instanceof String) {
                        username = (String) value;
                    } else {
                        if (value == null) {
                            // TODO: Add MigrationReport entry
                            printErr("Missing username for key " + key);
                            return;
                        }
                        // TODO: Add MigrationReport entry
                        printErr("Invalid value for username: " + value);
                        return;
                    }
                    break;
                case "email":
                    if (value instanceof String || value == null) {
                        email = (String) value;
                    } else {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid value for email: " + value);
                    }
                    break;
                case "full_name":
                    if (value instanceof String || value == null) {
                        fullName = (String) value;
                    } else {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid value for full_name: " + value);
                    }
                    break;
                // TODO: Add handling for role key
                // TODO: Add handling for metadata key
                // TODO: Add handling for enabled key
                default:
                    // TODO: Add MigrationReport entry
                    printErr("Unknown key" + key);
                    break;
            }
            print(key);
        }

        ir.addUser(new User(username, null, fullName, email));
    }

    private void readRole() {
        try {
            var reader = DocReader.json().read(roleFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }

            for (var entry : mapReader.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (!(key instanceof String)) {
                    // TODO: Add MigrationReport entry
                    printErr("Unexpected type for key: " + key);
                    continue;
                }

                var roleName = (String) entry.getKey();

                if (value instanceof LinkedHashMap<?, ?> role) {
                    readSingleRole(role, roleName);
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid value for role: " + value);
                }
            }

        } catch (Exception e) {
            // TODO: Add proper error handling
            printErr(e.getMessage());
        }
    }

    private void readSingleRole(LinkedHashMap<?, ?> roleMap, String roleName) {
        var role = new Role();
        role.setName(roleName);
        for (var entry : roleMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String)) {
                // TODO: Add MigrationReport entry
                printErr("Unexpected type for key: " + key);
                continue;
            }

            switch ((String) key) {
            case "applications":
                if (value instanceof ArrayList<?> applicationList) {
                    role.setApplications(readApplications(applicationList));
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for applications: " + value.getClass());
                }
                break;
            case "cluster":
                try {
                    role.setCluster(toStringArrayList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for cluster: " + key.getClass());
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for cluster entry: " + value.getClass());
                }
                break;
            case "indices":
                if (value instanceof ArrayList<?> indices) {
                    role.setIndices(readIndices(indices));
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for indices: " + value.getClass());
                }
                break;
            default:
                // TODO: Add MigrationReport entry
                printErr("Unknown key: " + key);
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
                    // TODO: Add MigrationReport entry
                    printErr("Application was not presented correctly.");
                }
            } else {
                // TODO: Add MigrationReport entry
                printErr("Invalid type for application: " + rawApplication.getClass());
            }
        }
        return applications;
    }

    private Role.Application readApplication(LinkedHashMap<?, ?> applicationMap) {
        var application = new Role.Application();
        for (var entry : applicationMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String)) {
                // TODO: Add MigrationReport entry
                printErr("Unexpected type for key: " + key);
                continue;
            }
            switch ((String) key) {
                case "application":
                    if (value instanceof String applicationName) {
                        application.setName(applicationName);
                    } else {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid value type for application: " + value.getClass());
                        return null;
                    }
                    break;
                case "privileges":
                    try {
                        application.setPrivileges(toStringArrayList(value));
                    } catch (IllegalArgumentException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for privileges: " + value.getClass());
                        return null;
                    } catch (ClassCastException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for value: " + e.getMessage());
                        return null;
                    }
                    break;
                case "resources":
                    try {
                        application.setResources(toStringArrayList(value));
                    } catch (IllegalArgumentException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for resources: " + value.getClass());
                        return null;
                    } catch (ClassCastException e) {
                        // TODO: Add MigrationReport entry
                        printErr("Invalid type for resources: " + e.getMessage());
                        return null;
                    }
                    break;
                default:
                    // TODO: Add MigrationReport entry
                    printErr("Unknown key: " + key);
                    break;
            }
        }
        if (application.getName() == null || application.getPrivileges() == null || application.getResources() == null) {
            // TODO: Add MigrationReport entry
            printErr("Application missing required parameter.");
            return null;
        }
        return application;
    }

    private List<Role.Index> readIndices(ArrayList<?> indexList) {
        var indices = new ArrayList<Role.Index>();
        for (var rawIndex : indexList) {
            if (rawIndex instanceof LinkedHashMap<?, ?> indexMap) {
                var index = readIndex(indexMap);
                if (index == null) {
                    // TODO: Add MigrationReport entry
                    printErr("Index was presented correctly.");
                    continue;
                }
                indices.add(index);
            } else {
                // TODO: Add MigrationReport entry
                printErr("Invalid type for index: " + rawIndex.getClass());
            }
        }
        return indices;
    }

    private Role.Index readIndex(LinkedHashMap<?, ?> indexMap) {
        var index = new Role.Index();
        for (var entry : indexMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String)) {
                // TODO: Add MigrationReport entry
                printErr("Unexpected type for key: " + key);
                return null;
            }

            // TODO: Add query key
            switch ((String) key) {
            case "field_security":
                if (value instanceof LinkedHashMap<?, ?> fieldSecurity) {
                    index.setFieldSecurity(readFieldSecurity(fieldSecurity));
                } else {
                    // TODO: Add MigrationReport entry
                }
                break;
            case "names":
                try {
                    index.setNames(toStringArrayList(value));
                } catch (IllegalArgumentException e) {
                    if (value instanceof String) {
                        var names = new ArrayList<String>();
                        names.add((String) value);
                        index.setNames(names);
                        break;
                    }
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for names: " + value.getClass());
                    return null;
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type found in names: " + e.getMessage());
                    return null;
                }
                break;
            case "privileges":
                try {
                    index.setPrivileges(toStringArrayList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for privileges: " + value.getClass());
                    return null;
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type found in privileges: " + e.getMessage());
                    return null;
                }
                break;
            case "allow_restricted_indices":
                if (value instanceof Boolean) {
                    index.setAllowRestrictedIndices((Boolean) value);
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Invalid type for allow_restricted_indices: " + key);
                }
            default:
                // TODO: Add MigrationReport entry
                printErr("Unknown key: " + key);
                break;
            }
        }
        if (index.getNames() == null || index.getNames().isEmpty() || index.getPrivileges() == null || index.getPrivileges().isEmpty()) {
            // TODO: Add MigrationReport entry
            printErr("Index missing required parameter.");
            return null;
        }
        return index;
    }

    private Role.FieldSecurity readFieldSecurity(LinkedHashMap<?, ?> fieldMap) {
        var fieldSecurity = new Role.FieldSecurity();
        for (var entry : fieldMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String)) {
                // TODO: Add MigrationReport entry
                printErr("Unexpected type for key: " + key);
            }
            switch ((String) key) {
            case "except":
                try {
                    fieldSecurity.setExcept(toStringArrayList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                }
                break;
            case "grant":
                try {
                    fieldSecurity.setGrant(toStringArrayList(value));
                } catch (IllegalArgumentException e) {
                    // TODO: Add MigrationReport entry
                } catch (ClassCastException e) {
                    // TODO: Add MigrationReport entry
                }
                break;
            }
        }
        return fieldSecurity;
    }

    private void readRoleMapping() {
        try {
            var reader = DocReader.json().read(roleMapping);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }

            for (var entry : mapReader.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (!(key instanceof String)) {
                    // TODO: Add MigrationReport entry
                    continue;
                }

                if ((value instanceof LinkedHashMap<?, ?> mapping)) {
                    readSingleRoleMapping(mapping);
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

    private void readSingleRoleMapping(LinkedHashMap<?, ?> mapping) {
        // TODO: Implement role mapping reading
        return;
    }

    public static ArrayList<String> toStringArrayList(Object obj) throws IllegalArgumentException, ClassCastException {
        if (!(obj instanceof List<?> list)) {
            throw new IllegalArgumentException("Object is not a List");
        }

        ArrayList<String> result = new ArrayList<>();

        for (Object element : list) {
            if (!(element instanceof String)) {
                throw new ClassCastException("" + element);
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
