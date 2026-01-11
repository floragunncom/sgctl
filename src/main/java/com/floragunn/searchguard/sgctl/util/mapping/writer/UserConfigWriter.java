package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes Search Guard user definitions derived from the intermediate representation.
 */
public class UserConfigWriter implements Document<UserConfigWriter> {
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGInternalUser> users;

    static final String FILE_NAME = "sg_internal_users.yml";

    public UserConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.users = new ArrayList<>(ir.getUsers().size());
        createSGInternalUser();
    }

    private void createSGInternalUser() {
        for (var user : ir.getUsers()) {
            // TODO: Add handling for enabled
            if (!user.getEnabled()) {
                report.addWarning(FILE_NAME,
                        user.getUsername() + "->enabled",
                        "The user has 'enabled' set to false. This can not be done in Search Guard and the user is not migrated.");
                continue;
            }
            var attributes = new LinkedHashMap<>(user.getAttributes());
            if (user.getEmail() != null) {
                attributes.put("email", user.getEmail());
            }
            if (user.getFullName() != null) {
                attributes.put("full_name", user.getFullName());
            }
            if (user.getProfileUID() != null) {
                attributes.put("profile_uid", user.getProfileUID());
            }
            users.add(new SGInternalUser(user.getUsername(), "", attributes, user.getRoles()));
            report.addManualAction(
                    FILE_NAME,
                    user.getUsername() + "->hash",
                    "Password hashes can not be exported from X-Pack. The hash has to be set manually at the field marked with 'change it'"
            );
        }
    }

    @Override
    public Object toBasicObject() {
        var map = new LinkedHashMap<String, SGInternalUser>();
        for (var user : users) {
            map.put(user.name, user);
        }
        return map;
    }

    static class SGInternalUser implements Document<SGInternalUser> {
        String name;
        String hash;
        String description;
        Map<String, Object> attributes;
        List<String> roles;

        public SGInternalUser(String name, String description, Map<String, Object> attributes, List<String> roles) {
            this.name = name;
            this.hash = "Change it";
            this.description = description;
            this.attributes = attributes;
            this.roles = roles;
        }

        @Override
        public Object toBasicObject() {
            LinkedHashMap<String, Object> contents = new LinkedHashMap<>();
            contents.put("hash", hash);
            contents.put("search_guard_roles", roles);
            contents.put("attributes", attributes);
            contents.put("description", description);
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
