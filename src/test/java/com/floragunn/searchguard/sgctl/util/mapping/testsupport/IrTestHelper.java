package com.floragunn.searchguard.sgctl.util.mapping.testsupport;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper methods for creating common IR objects in tests.
 */
public final class IrTestHelper {
    private IrTestHelper() {
    }

    /**
     * Creates a minimal user with roles and attributes.
     *
     * @param username user name
     * @param roles user roles
     * @param enabled enabled flag
     * @return user instance
     */
    public static User newUser(String username, List<String> roles, boolean enabled) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("source", "test");
        return new User(username, roles, null, null, enabled, null, new LinkedHashMap<>(attributes));
    }

    /**
     * Creates a role with cluster permissions and indices.
     *
     * @param name role name
     * @param cluster cluster permissions
     * @param indices indices list
     * @return role instance
     */
    public static Role newRole(String name, List<String> cluster, List<Role.Index> indices) {
        Role role = new Role(name);
        role.setCluster(cluster);
        role.setIndices(indices);
        return role;
    }

    /**
     * Creates a role mapping for the given roles and rules.
     *
     * @param name mapping name
     * @param roles roles list
     * @param rules rules definition
     * @return role mapping instance
     */
    public static RoleMapping newRoleMapping(String name, List<String> roles, RoleMapping.Rules rules) {
        RoleMapping mapping = new RoleMapping(name);
        mapping.setRoles(roles);
        mapping.setRules(rules);
        return mapping;
    }

    /**
     * Creates an intermediate representation populated with provided entries.
     *
     * @param role role to add
     * @param user user to add
     * @param mapping mapping to add
     * @return populated intermediate representation
     */
    public static IntermediateRepresentation newIr(Role role, User user, RoleMapping mapping) {
        IntermediateRepresentation ir = new IntermediateRepresentation();
        if (role != null) {
            ir.addRole(role);
        }
        if (user != null) {
            ir.addUser(user);
        }
        if (mapping != null) {
            ir.addRoleMapping(mapping);
        }
        return ir;
    }
}
