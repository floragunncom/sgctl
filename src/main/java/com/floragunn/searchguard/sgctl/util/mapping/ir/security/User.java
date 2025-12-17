package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class User {
    @NonNull private String username;
    private List<String> roles;
    private Boolean enabled;
    private Map<String, Object> attributes;
    private String fullName;
    private String email;
    private String profileUID;

    public User(@NonNull String username, List<String> roles, String fullName, String email, Boolean enabled, String profileUID, LinkedHashMap<String, Object> attributes) {
        this.username = username;
        this.roles = freezeList(roles);
        this.fullName = fullName;
        this.email = email;
        this.enabled = enabled;
        this.profileUID = profileUID;
        this.attributes = freezeMap(attributes);
    }

    // Getter-Methods
    public @NonNull String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Map<String, Object> getAttributes() { return attributes; }
    public Boolean getEnabled() { return enabled; }
    public String getProfileUID() { return profileUID; }

    // Setter-Methods
    public void setUsername(@NonNull String username) { this.username = username; }
    public void setRoles(List<String> roles) { this.roles = freezeList(roles); }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileUID(String profileUID) { this.profileUID = profileUID; }
    public void setAttributes(LinkedHashMap<String, Object> attributes) { this.attributes = freezeMap(attributes); }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    private static List<String> freezeList(List<String> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static Map<String, Object> freezeMap(LinkedHashMap<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    @Override
    public String toString() {
        return "User [\n\tusername=" + username + "\n\troles=" + roles + "\n\tfullName=" + fullName + "\n\temail=" + email + "\n\tenabled=" + enabled + "\n\tattributes=" + attributes + "\n]";
    }
}
