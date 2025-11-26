package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class User {
    @NonNull String username;
    List<String> roles; // TODO: change datatype to be able reference the role datatype
    String fullName;
    String email;

    public User(@NonNull String username) {
        this.username = username;
    }

    // Getter-Methods
    public @NonNull String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    // Setter-Methods
    public void setUsername(@NonNull String username) { this.username = username; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User [username=" + username + ", roles=" + roles + ", fullName=" + fullName + ", email=" + email + "]";
    }
}
