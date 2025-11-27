package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.toStringList;

public class UserConfigReader {
    File userFile;
    IntermediateRepresentation ir;
    MigrationReport report;

    static final String FILE_NAME = "user.json";

    public UserConfigReader(File userFile, IntermediateRepresentation ir, MigrationReport report) {
        this.userFile = userFile;
        this.ir = ir;
        this.report = report;
        readUserFile();
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
        List<String> roles = null;
        Boolean enabled = null;
        String fullName = null;
        String email = null;
        String profileUID = null;
        LinkedHashMap<String, Object> attributes = null;

        for (var entry : userMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                report.addInvalidType(FILE_NAME, name, String.class.getTypeName(), entry.getKey().getClass().getTypeName());
                continue;
            }
            var value = entry.getValue();
            final var path = name + "->" + key;
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
                        email = (String) value;
                    } else {
                        report.addInvalidType(FILE_NAME, path, String.class.getTypeName(), value.getClass().getTypeName());
                    }
                    break;
                case "full_name":
                    if (value instanceof String || value == null) {
                        fullName = (String) value;
                    } else {
                        report.addInvalidType(FILE_NAME, path, String.class.getTypeName(), value.getClass().getTypeName());
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
                        report.addInvalidType(FILE_NAME, name + "->" + key, LinkedHashMap.class.getTypeName(), value.getClass().getTypeName());
                    }
                    break;
                case "roles":
                    var uncheckedRoles = toStringList(value, FILE_NAME, name, key);
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
                    } else {
                        report.addInvalidType(FILE_NAME, name + "->" + key, Boolean.class.getTypeName(), value.getClass().getTypeName());
                    }
                    break;
                case "profile_uid":
                    if (value instanceof String uid) {
                        profileUID = uid;
                    } else {
                        report.addInvalidType(FILE_NAME, name + "->" + key, String.class.getTypeName(), value.getClass().getTypeName());
                    }
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

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
