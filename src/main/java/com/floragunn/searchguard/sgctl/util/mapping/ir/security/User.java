package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class User {
    @NonNull private String username;
    private final List<String> roles = new ArrayList<>();
    private final List<String> rolesView = Collections.unmodifiableList(roles);
    private boolean rolesSet;
    private Boolean enabled;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final Map<String, Object> attributesView = Collections.unmodifiableMap(attributes);
    private boolean attributesSet;
    private String fullName;
    private String email;
    private String profileUID;

    public User(@NonNull String username, List<String> roles, String fullName, String email, Boolean enabled, String profileUID, LinkedHashMap<String, Object> attributes) {
        this.username = username;
        setRoles(roles);
        this.fullName = fullName;
        this.email = email;
        this.enabled = enabled;
        this.profileUID = profileUID;
        setAttributes(attributes);
    }

    // Getter-Methods
    public @NonNull String getUsername() { return username; }
    public List<String> getRoles() { return rolesSet ? rolesView : null; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Map<String, Object> getAttributes() { return attributesSet ? attributesView : null; }
    public Boolean getEnabled() { return enabled; }
    public String getProfileUID() { return profileUID; }

    // Setter-Methods
    public void setUsername(@NonNull String username) { this.username = username; }
    public void setRoles(List<String> roles) { replaceRoles(roles); }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileUID(String profileUID) { this.profileUID = profileUID; }
    public void setAttributes(LinkedHashMap<String, Object> attributes) { replaceAttributes(attributes); }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    private void replaceRoles(List<String> list) {
        if (list == null) {
            roles.clear();
            rolesSet = false;
            return;
        }
        rolesSet = true;
        roles.clear();
        roles.addAll(list);
    }

    private void replaceAttributes(LinkedHashMap<String, Object> map) {
        if (map == null) {
            attributes.clear();
            attributesSet = false;
            return;
        }
        attributesSet = true;
        attributes.clear();
        attributes.putAll(map);
    }

    @Override
    public String toString() {
        return "User [\n\tusername=" + username + "\n\troles=" + roles + "\n\tfullName=" + fullName + "\n\temail=" + email + "\n\tenabled=" + enabled + "\n\tattributes=" + attributes + "\n]";
    }
}
