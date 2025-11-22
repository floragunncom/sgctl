package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.List;

public class User {
    String username;
    List<String> roles; // TODO change datatype to be able reference the role datatype
    String fullName;
    String email;

    public User(String username, List<String> roles, String fullName, String email) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }
}
