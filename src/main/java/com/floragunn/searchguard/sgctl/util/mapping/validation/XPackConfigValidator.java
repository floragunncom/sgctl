/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


package com.floragunn.searchguard.sgctl.util.mapping.validation;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs basic consistency checks on the intermediate representation
 * built from X-Pack configuration input.
 */
public final class XPackConfigValidator {

    private static final String COMPONENT_GLOBAL = "global";
    private static final String COMPONENT_ROLE = "role";
    private static final String COMPONENT_USER = "user";
    private static final String COMPONENT_ROLE_MAPPING = "role-mapping";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_ROLES = "roles";
    private static final String FIELD_MAPPING_NAME = "mappingName";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_USERS = "users";

    private XPackConfigValidator() {
        // utility class
    }

    /**
     * Validates the given intermediate representation and returns a result containing all issues.
     *
     * @param ir intermediate representation produced by the X-Pack reader
     * @return validation result with all detected issues
     */
    public static XPackValidationResult validate(IntermediateRepresentation ir) {
        XPackValidationResult result = new XPackValidationResult();

        if (ir == null) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_GLOBAL,
                    null,
                    null,
                    "Intermediate representation must not be null"
            ));
            return result;
        }

        Map<String, Role> rolesByName = validateRoles(ir, result);
        Map<String, User> usersByName = validateUsers(ir, result);
        validateRoleMappings(ir, rolesByName, usersByName, result);

        return result;
    }

    /**
     * Validates roles and returns a lookup map used by subsequent checks.
     *
     * @param ir intermediate representation
     * @param result validation result to fill
     * @return map from role name to role instance
     */
    private static Map<String, Role> validateRoles(
            IntermediateRepresentation ir,
            XPackValidationResult result
    ) {
        Map<String, Role> rolesByName = new HashMap<>();
        Set<String> duplicateNames = new HashSet<>();

        for (Role role : ir.getRoles()) {
            validateSingleRole(role, rolesByName, duplicateNames, result);
        }

        return rolesByName;
    }

    private static void validateSingleRole(
            Role role,
            Map<String, Role> rolesByName,
            Set<String> duplicateNames,
            XPackValidationResult result
    ) {
        if (role == null) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE,
                    null,
                    null,
                    "Encountered null role entry in intermediate representation"
            ));
            return;
        }

        String name = role.getName();
        if (name.isBlank()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE,
                    null,
                    FIELD_NAME,
                    "Role name must not be null or blank"
            ));
            return;
        }

        if (!rolesByName.containsKey(name)) {
            rolesByName.put(name, role);
        } else if (duplicateNames.add(name)) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE,
                    name,
                    FIELD_NAME,
                    "Duplicate role name detected"
            ));
        }

        List<String> clusterPrivileges = role.getCluster();
        List<Role.Index> indices = role.getIndices();
        List<Role.RemoteIndex> remoteIndices = role.getRemoteIndices();

        boolean hasCluster = clusterPrivileges != null && !clusterPrivileges.isEmpty();
        boolean hasIndices = indices != null && !indices.isEmpty();
        boolean hasRemoteIndices = remoteIndices != null && !remoteIndices.isEmpty();

        if (!hasCluster && !hasIndices && !hasRemoteIndices) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.WARNING,
                    COMPONENT_ROLE,
                    name,
                    null,
                    "Role defines neither cluster nor indices nor remote_indices privileges"
            ));
        }
    }

    /**
     * Validates users and returns a lookup map used by subsequent checks.
     *
     * @param ir intermediate representation
     * @param result validation result to fill
     * @return map from username to user instance
     */
    private static Map<String, User> validateUsers(
            IntermediateRepresentation ir,
            XPackValidationResult result
    ) {
        Map<String, User> usersByName = new HashMap<>();
        Set<String> duplicateNames = new HashSet<>();

        for (User user : ir.getUsers()) {
            validateSingleUser(user, usersByName, duplicateNames, result);
        }

        return usersByName;
    }

    private static void validateSingleUser(
            User user,
            Map<String, User> usersByName,
            Set<String> duplicateNames,
            XPackValidationResult result
    ) {
        if (user == null) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_USER,
                    null,
                    null,
                    "Encountered null user entry in intermediate representation"
            ));
            return;
        }

        String username = user.getUsername();
        if (username.isBlank()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_USER,
                    null,
                    FIELD_USERNAME,
                    "Username must not be null or blank"
            ));
            return;
        }

        if (!usersByName.containsKey(username)) {
            usersByName.put(username, user);
        } else if (duplicateNames.add(username)) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_USER,
                    username,
                    FIELD_USERNAME,
                    "Duplicate username detected"
            ));
        }

        List<String> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.WARNING,
                    COMPONENT_USER,
                    username,
                    FIELD_ROLES,
                    "User has no roles assigned"
            ));
        }
    }

    /**
     * Validates role mappings using the lookup maps from roles and users.
     *
     * @param ir intermediate representation
     * @param rolesByName lookup of existing roles
     * @param usersByName lookup of existing users
     * @param result validation result to fill
     */
    private static void validateRoleMappings(
            IntermediateRepresentation ir,
            Map<String, Role> rolesByName,
            Map<String, User> usersByName,
            XPackValidationResult result
    ) {
        Set<String> mappingNames = new HashSet<>();

        for (RoleMapping mapping : ir.getRoleMappings()) {
            validateSingleRoleMapping(mapping, rolesByName, usersByName, mappingNames, result);
        }
    }

    private static void validateSingleRoleMapping(
            RoleMapping mapping,
            Map<String, Role> rolesByName,
            Map<String, User> usersByName,
            Set<String> mappingNames,
            XPackValidationResult result
    ) {
        if (mapping == null) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE_MAPPING,
                    null,
                    null,
                    "Encountered null role mapping entry in intermediate representation"
            ));
            return;
        }

        String name = mapping.getMappingName();

        validateRoleMappingName(name, mappingNames, result);
        validateRoleMappingEnabledFlag(name, mapping, result);
        validateRoleMappingRoles(name, mapping, rolesByName, result);
        validateRoleMappingUsers(name, mapping, usersByName, result);
    }

    private static void validateRoleMappingName(
            String name,
            Set<String> mappingNames,
            XPackValidationResult result
    ) {
        if (name == null || name.isBlank()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE_MAPPING,
                    null,
                    FIELD_MAPPING_NAME,
                    "Role mapping name must not be null or blank"
            ));
        } else if (!mappingNames.add(name)) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE_MAPPING,
                    name,
                    FIELD_MAPPING_NAME,
                    "Duplicate role mapping name detected"
            ));
        }
    }

    private static void validateRoleMappingEnabledFlag(
            String mappingName,
            RoleMapping mapping,
            XPackValidationResult result
    ) {
        if (!mapping.isEnabled()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.WARNING,
                    COMPONENT_ROLE_MAPPING,
                    mappingName,
                    FIELD_ENABLED,
                    "Role mapping is disabled and will not be used during migration"
            ));
        }
    }

    private static void validateRoleMappingRoles(
            String mappingName,
            RoleMapping mapping,
            Map<String, Role> rolesByName,
            XPackValidationResult result
    ) {
        List<String> roles = mapping.getRoles();
        if (roles == null || roles.isEmpty()) {
            result.addIssue(new XPackValidationIssue(
                    XPackValidationSeverity.ERROR,
                    COMPONENT_ROLE_MAPPING,
                    mappingName,
                    FIELD_ROLES,
                    "Role mapping must reference at least one role"
            ));
            return;
        }

        for (String roleName : roles) {
            if (!rolesByName.containsKey(roleName)) {
                result.addIssue(new XPackValidationIssue(
                        XPackValidationSeverity.ERROR,
                        COMPONENT_ROLE_MAPPING,
                        mappingName,
                        FIELD_ROLES,
                        "Referenced role '" + roleName + "' does not exist in roles definition"
                ));
            }
        }
    }

    private static void validateRoleMappingUsers(
            String mappingName,
            RoleMapping mapping,
            Map<String, User> usersByName,
            XPackValidationResult result
    ) {
        List<String> users = mapping.getUsers();
        if (users == null) {
            return;
        }

        for (String username : users) {
            if (!usersByName.containsKey(username)) {
                result.addIssue(new XPackValidationIssue(
                        XPackValidationSeverity.WARNING,
                        COMPONENT_ROLE_MAPPING,
                        mappingName,
                        FIELD_USERS,
                        "Referenced user '" + username + "' does not exist in users definition"
                ));
            }
        }
    }
}
