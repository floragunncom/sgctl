package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.List;

public class IntermediateRepresentation {
    List<User> users = new ArrayList<>();
    List<Role> roles = new ArrayList<>();

    // Getter-Methods
    public void addUser(User user) { users.add(user); }
    public void addRole(Role role) { roles.add(role); }

    // Setter-Methods
    public List<User> getUsers() { return users; }
    public List<Role> getRoles() { return roles; }

}