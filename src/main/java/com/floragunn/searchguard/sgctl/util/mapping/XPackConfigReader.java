package com.floragunn.searchguard.sgctl.util.mapping;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.InteremediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.User;

import java.io.File;
import java.util.LinkedHashMap;

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File roleMapping;
    InteremediateRepresentation ir;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMapping) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMapping = roleMapping;
        this.ir = new InteremediateRepresentation();
    }

    public InteremediateRepresentation generateIR() {
        readUser();
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
                    // TODO: Checking for to us unknown keys
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
            }

        } catch (Exception e) {
            // TODO: Add proper error handling
            printErr(e.getMessage());
        }
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
            }

        } catch (Exception e) {
            // TODO: Add proper error handling
            printErr(e.getMessage());
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
