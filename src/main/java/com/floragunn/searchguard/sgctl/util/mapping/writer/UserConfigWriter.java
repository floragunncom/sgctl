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

    /**
     * Creates a writer that converts users from the intermediate representation
     * into Search Guard internal user definitions.
     *
     * @param ir the intermediate representation containing user definitions
     */
    public UserConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.users = new ArrayList<>(ir.getUsers().size());
        createSGInternalUser();
    }

    /**
     * Converts all users from the intermediate representation into internal
     * Search Guard user objects.
     * <p>
     * Disabled users are not migrated and will result in a warning.
     * Password hashes cannot be migrated and must be set manually.
     */
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

        /**
         * Creates a Search Guard internal user definition.
         * <p>
         * The password hash is intentionally set to a placeholder value and must
         * be replaced manually after migration.
         *
         * @param name        the username
         * @param description a textual description of the user
         * @param attributes  user attributes such as email or full name
         * @param roles       assigned Search Guard roles
         */
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
}
