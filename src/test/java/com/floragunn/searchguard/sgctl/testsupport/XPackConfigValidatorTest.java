package com.floragunn.searchguard.sgctl.testsupport;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.User;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackConfigValidator;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackValidationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XPackConfigValidator}.
 */
class XPackConfigValidatorTest {

    /**
     * Verifies that passing a null intermediate representation produces a single error.
     */
    @Test
    void shouldReportErrorWhenIntermediateRepresentationIsNull() {
        XPackValidationResult result = XPackConfigValidator.validate(null);

        assertTrue(result.hasErrors());
        assertEquals(1, result.getIssues().size());

        String msg = result.getIssues().get(0).toString();
        assertTrue(msg.contains("global"));
        assertTrue(msg.contains("Intermediate representation must not be null"));
    }

    /**
     * Verifies that a null role entry in the roles list is reported as an error.
     */
    @Test
    void shouldReportErrorForNullRoleEntries() {
        List<Role> roles = new ArrayList<>();
        roles.add(null);

        IntermediateRepresentation ir = createIr(roles, Collections.emptyList(), Collections.emptyList());

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
                .map(Object::toString)
                .anyMatch(s -> s.contains("Encountered null role entry in intermediate representation")));
    }

    /**
     * Verifies that duplicate role names and roles without any privileges are reported.
     */
    @Test
    void shouldValidateRolesAndDetectDuplicateNamesAndMissingPrivileges() {
        Role roleWithPrivileges = new Role("admin");
        roleWithPrivileges.setCluster(Collections.singletonList("monitor"));

        Role duplicateRoleWithoutPrivileges = new Role("admin");

        List<Role> roles = new ArrayList<>();
        roles.add(roleWithPrivileges);
        roles.add(duplicateRoleWithoutPrivileges);

        IntermediateRepresentation ir = createIr(roles, Collections.emptyList(), Collections.emptyList());

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertEquals(2, result.getIssues().size());

        List<String> messages = result.getIssues().stream()
                .map(Object::toString)
                .toList();

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("Duplicate role name detected")));

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("neither cluster nor indices nor remote_indices privileges")));
    }

    /**
     * Verifies that null user entries and users without roles are reported.
     */
    @Test
    void shouldReportNullUsersAndUsersWithoutRoles() {
        User userWithoutRoles = new User("alice");
        userWithoutRoles.setRoles(null);

        List<User> users = new ArrayList<>();
        users.add(null);
        users.add(userWithoutRoles);

        IntermediateRepresentation ir = createIr(Collections.emptyList(), users, Collections.emptyList());

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.hasWarnings());

        List<String> messages = result.getIssues().stream()
                .map(Object::toString)
                .toList();

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("Encountered null user entry in intermediate representation")));

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("User has no roles assigned")));
    }

    /**
     * Verifies that duplicate usernames are reported as errors.
     */
    @Test
    void shouldDetectDuplicateUsernames() {
        User first = new User("alice");
        User second = new User("alice");

        List<User> users = new ArrayList<>();
        users.add(first);
        users.add(second);

        IntermediateRepresentation ir = createIr(Collections.emptyList(), users, Collections.emptyList());

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
                .map(Object::toString)
                .anyMatch(s -> s.contains("Duplicate username detected")));
    }

    /**
     * Verifies that duplicate role mapping names are reported as errors.
     */
    @Test
    void shouldDetectDuplicateRoleMappingNames() {
        RoleMapping first = new RoleMapping("mapping1");
        first.setEnabled(true);
        first.setRoles(Collections.singletonList("admin"));

        RoleMapping second = new RoleMapping("mapping1");
        second.setEnabled(true);
        second.setRoles(Collections.singletonList("admin"));

        List<RoleMapping> mappings = new ArrayList<>();
        mappings.add(first);
        mappings.add(second);

        IntermediateRepresentation ir = createIr(Collections.emptyList(), Collections.emptyList(), mappings);

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
                .map(Object::toString)
                .anyMatch(s -> s.contains("Duplicate role mapping name detected")));
    }

    /**
     * Verifies that disabled mappings, missing roles and unknown references are reported.
     */
    @Test
    void shouldDetectDisabledMappingsMissingRolesAndUnknownReferences() {
        Role existingRole = new Role("admin");

        User existingUser = new User("alice");
        existingUser.setRoles(Collections.singletonList("admin"));

        RoleMapping mapping = new RoleMapping("mappingWithIssues");
        mapping.setEnabled(false);
        mapping.setRoles(Collections.singletonList("does-not-exist"));
        mapping.setUsers(Collections.singletonList("bob"));

        List<Role> roles = Collections.singletonList(existingRole);
        List<User> users = Collections.singletonList(existingUser);
        List<RoleMapping> mappings = Collections.singletonList(mapping);

        IntermediateRepresentation ir = createIr(roles, users, mappings);

        XPackValidationResult result = XPackConfigValidator.validate(ir);

        assertTrue(result.hasErrors());
        assertTrue(result.hasWarnings());

        List<String> messages = result.getIssues().stream()
                .map(Object::toString)
                .toList();

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("Role mapping is disabled and will not be used during migration")));

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("Referenced role 'does-not-exist' does not exist")));

        assertTrue(messages.stream()
                .anyMatch(s -> s.contains("Referenced user 'bob' does not exist")));
    }

    /**
     * Creates a minimal intermediate representation from the given lists.
     * This version does not rely on setters or special constructors in
     * {@link IntermediateRepresentation}, but uses its internal collections.
     */
    private IntermediateRepresentation createIr(
            List<Role> roles,
            List<User> users,
            List<RoleMapping> mappings
    ) {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        if (roles != null) {
            ir.getRoles().addAll(roles);
        }
        if (users != null) {
            ir.getUsers().addAll(users);
        }
        if (mappings != null) {
            ir.getRoleMappings().addAll(mappings);
        }

        return ir;
    }
}
