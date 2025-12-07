package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;

public class User {
    @NonNull private String username;
    private List<String> roles;
    private Boolean enabled;
    private LinkedHashMap<String, Object> attributes;
    private String fullName;
    private String email;
    private String profileUID;

    public User(@NonNull String username, List<String> roles, String fullName, String email, Boolean enabled, String profileUID, LinkedHashMap<String, Object> attributes) {
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
    public List<String> getRoles() { return roles; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public LinkedHashMap<String, Object> getAttributes() { return attributes; }
    public Boolean getEnabled() { return enabled; }
    public String getProfileUID() { return profileUID; }

    // Setter-Methods
    public void setUsername(@NonNull String username) { this.username = username; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileUID(String profileUID) { this.profileUID = profileUID; }
    public void setAttributes(LinkedHashMap<String, Object> attributes) { this.attributes = attributes; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return "User [\n\tusername=" + username + "\n\troles=" + roles + "\n\tfullName=" + fullName + "\n\temail=" + email + "\n\tenabled=" + enabled + "\n\tattributes=" + attributes + "\n]";
    }
}
