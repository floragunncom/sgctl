package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the in-memory representation of users, roles, role mappings, and elasticsearch.yml settings during migration.
 */
public class IntermediateRepresentation {
    private final List<User> users = new ArrayList<>();
    private final List<User> usersView = Collections.unmodifiableList(users);
    private final List<Role> roles = new ArrayList<>();
    private final List<Role> rolesView = Collections.unmodifiableList(roles);
    private final List<RoleMapping> roleMappings = new ArrayList<RoleMapping>();
    private final List<RoleMapping> roleMappingsView = Collections.unmodifiableList(roleMappings);
    private final IntermediateRepresentationElasticSearchYml elasticSearchYml = new IntermediateRepresentationElasticSearchYml();
    private boolean frozen;

    // Setter-Methods
    public void addUser(User user) {
        ensureMutable();
        users.add(user);
    }

    public void addRole(Role role) {
        ensureMutable();
        roles.add(role);
    }

    public void addRoleMapping(RoleMapping roleMapping) {
        ensureMutable();
        roleMappings.add(roleMapping);
    }


    // Getter-Methods
    public List<User> getUsers() { return usersView; }
    public List<Role> getRoles() { return rolesView; }
    public List<RoleMapping> getRoleMappings() { return roleMappingsView; }
    public IntermediateRepresentationElasticSearchYml getElasticSearchYml() { return elasticSearchYml; }

    public IntermediateRepresentation freeze() {
        frozen = true;
        return this;
    }

    private void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("IntermediateRepresentation is frozen");
        }
    }
}
