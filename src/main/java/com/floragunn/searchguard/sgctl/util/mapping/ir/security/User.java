package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;

public class User {
    @NonNull String username;
    @NonNull List<String> roles; // TODO: change datatype to be able reference the role datatype
    @NonNull Boolean enabled;
    @NonNull LinkedHashMap<String, Object> attributes;
    String fullName;
    String email;
    String profileUID;

    public User(@NonNull String username, @NonNull List<String> roles, String fullName, String email, @NonNull Boolean enabled, String profileUID, @NonNull LinkedHashMap<String, Object> attributes) {
        this.username = username;
        this.roles = roles;
        this.fullName = fullName;
        this.email = email;
        this.enabled = enabled;
        this.profileUID = profileUID;
        this.attributes = attributes;
    }

    // Getter-Methods
    public @NonNull String getUsername() { return username; }
    public @NonNull List<String> getRoles() { return roles; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public @NonNull LinkedHashMap<String, Object> getAttributes() { return attributes; }
    public @NonNull Boolean getEnabled() { return enabled; }
    public String getProfileUID() { return profileUID; }

    // Setter-Methods
    public void setUsername(@NonNull String username) { this.username = username; }
    public void setRoles(@NonNull List<String> roles) { this.roles = roles; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileUID(String profileUID) { this.profileUID = profileUID; }
    public void setAttributes(@NonNull LinkedHashMap<String, Object> attributes) { this.attributes = attributes; }
    public void setEnabled(@NonNull Boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return "User [\n\tusername=" + username + "\n\troles=" + roles + "\n\tfullName=" + fullName + "\n\temail=" + email + "\n\tenabled=" + enabled + "\n\tattributes=" + attributes + "\n]";
    }
}
