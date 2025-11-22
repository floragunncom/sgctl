package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.List;

public class IntermediateRepresentation {
    List<User> users = new ArrayList<>();
    List<Role> roles = new ArrayList<>();
    List<RoleMapping> roleMappings = new ArrayList<RoleMapping>();

    // Getter-Methods
    public void addUser(User user) { users.add(user); }
    public void addRole(Role role) { roles.add(role); }
    public void addRoleMapping(RoleMapping roleMapping) { roleMappings.add(roleMapping); }

    // Setter-Methods
    public List<User> getUsers() { return users; }
    public List<Role> getRoles() { return roles; }
    public List<RoleMapping> getRoleMappings() { return roleMappings; }


}