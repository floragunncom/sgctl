package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.List;

public class InteremediateRepresentation {
    List<User> users = new ArrayList<User>();

    public void  addUser(User user) { users.add(user); }
    public List<User> getUsers() { return users; }
}
