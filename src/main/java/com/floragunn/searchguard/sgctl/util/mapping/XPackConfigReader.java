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
        readRoleMapping();
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

    private void readSingleRole(LinkedHashMap<?, ?> role, String roleName) {

        for (var entry : role.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String)) {
                // TODO: Add MigrationReport entry
                printErr("Unexpected type for key: " + key);
                continue;
            }

            switch ((String) key) {
            case "applications":
                print(value);
                if (value instanceof ArrayList<?> applicationList) {
                    var applications = readApplications(applicationList);
                }
                break;
            case "cluster":
                break;
            case "":
                break;
            default:
                // TODO: Add MigrationReport entry
                printErr("Unknown key: " + key);
            }
        }

        ir.addRole(new Role());
    }

    // TODO: Change 'String' to 'Application'-Object
    private List<Role.Application> readApplications(ArrayList<?> applicationList) {
        var applications = new ArrayList<Role.Application>();
        for (var application : applicationList) {
            if (application instanceof LinkedHashMap<?, ?> applicationMap) {
                var app = readApplication(applicationMap);
                if  (app != null) {
                    applications.add(app);
                } else {
                    // TODO: Add MigrationReport entry
                    printErr("Application was not presented correctly.");
                }
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
            }
        }
        return application;
    }

//    private List<String> readCluster(LinkedHashMap<?, ?> cluster) {
//
//    }

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
