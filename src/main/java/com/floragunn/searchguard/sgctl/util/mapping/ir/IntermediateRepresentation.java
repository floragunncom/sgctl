package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;

import java.util.ArrayList;
import java.util.List;

public class IntermediateRepresentation {
    List<User> users = new ArrayList<>();
    List<Role> roles = new ArrayList<>();
    List<RoleMapping> roleMappings = new ArrayList<RoleMapping>();
    IntermediateRepresentationElasticSearchYml elasticSearchYml = new IntermediateRepresentationElasticSearchYml();

    // Setter-Methods
    public void addUser(User user) { users.add(user); }
    public void addRole(Role role) { roles.add(role); }
    public void addRoleMapping(RoleMapping roleMapping) { roleMappings.add(roleMapping); }


    // Getter-Methods
    public List<User> getUsers() { return users; }
    public List<Role> getRoles() { return roles; }
    public List<RoleMapping> getRoleMappings() { return roleMappings; }
    public IntermediateRepresentationElasticSearchYml getElasticSearchYml() { return elasticSearchYml; }
}