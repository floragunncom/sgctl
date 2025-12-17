package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.toStringList;

public class UserConfigReader {
    private final File userFile;
    private final IntermediateRepresentation ir;
    private final MigrationReport report;

    static final String FILE_NAME = "user.json";

    public UserConfigReader(File userFile, IntermediateRepresentation ir) {
        this.userFile = userFile;
        this.ir = ir;
        this.report = MigrationReport.shared;
        try {
            readUserFile();
        } catch (DocumentParseException | IOException e) {
            report.addWarning(FILE_NAME, "origin", e.getMessage());
        }
    }

    private void readUserFile() throws DocumentParseException, IOException {
        if (userFile == null) return;
        var reader = DocReader.json().read(userFile);
        if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
            report.addInvalidType(FILE_NAME, "origin", LinkedHashMap.class, reader);
            return;
        }
        readUsers(mapReader);
    }

    private void readUsers(LinkedHashMap<?, ?> mapReader) {
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                report.addInvalidType(FILE_NAME, "origin", String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();

            if (value instanceof LinkedHashMap<?, ?> user) {
                readUser(user, key);
            } else {
                report.addInvalidType(FILE_NAME, "origin", LinkedHashMap.class, value);
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
                report.addInvalidType(FILE_NAME, name, String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();
            final var origin = name + "->" + key;
            switch (key) {
                case "username":
                    if (value instanceof String username) {
                        if (!name.equals(username)) {
                            report.addWarning(FILE_NAME, origin, "The key of the user does not match the username attribute. Key: '" + name + "' username: '" + username + "'");
                            return;
                        }
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                        return;
                    }
                    break;
                case "email":
                    if (value instanceof String || value == null) {
                        email = (String) value;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                case "full_name":
                    if (value instanceof String || value == null) {
                        fullName = (String) value;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                case "metadata":
                    if (value instanceof LinkedHashMap<?, ?> metadata) {
                        if (metadata.keySet().stream().allMatch(String.class::isInstance)) {
                            @SuppressWarnings("unchecked") // Cast is logically checked to always be possible
                            var safeMap = (LinkedHashMap<String, Object>) metadata;
                            attributes = safeMap;
                        } else {
                            report.addInvalidType(FILE_NAME, origin, LinkedHashMap.class, value);
                        }
                    } else {
                        report.addInvalidType(FILE_NAME, origin, LinkedHashMap.class, value);
                    }
                    break;
                case "roles":
                    var uncheckedRoles = toStringList(value, FILE_NAME, name, key);
                    if (uncheckedRoles == null) {
                        break;
                    }
                    var checkedRoles = new ArrayList<String>();
                    uncheckedRoles.forEach(role -> {
                        if (ir.getRoles().contains(new Role(role))) {
                            checkedRoles.add(role);
                        } else {
                            report.addWarning(FILE_NAME, origin + "->roles", "Role '" + role + "' does not exist in the role.json file.");
                        }
                    });
                    roles = checkedRoles;
                    break;
                case "enabled":
                    if (value instanceof Boolean) {
                        enabled = (Boolean) value;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, Boolean.class, value);
                    }
                    break;
                case "profile_uid":
                    if (value instanceof String uid) {
                        profileUID = uid;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                default:
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
            }
        }

        if (enabled == null) {
            report.addMissingParameter(FILE_NAME, "enabled", name);
        }
        if (roles == null) {
            report.addMissingParameter(FILE_NAME, "roles", name);
        }
        if (attributes == null) {
            report.addMissingParameter(FILE_NAME, "attributes", name);
            attributes = new LinkedHashMap<>();
        }

        var user = new User(name, roles, fullName, email, enabled, profileUID, attributes);
        ir.addUser(user);
    }
}
