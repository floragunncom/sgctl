package com.floragunn.searchguard.sgctl.util.mapping.ir;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class RoleMapping {
    @NonNull String mappingName;
    List<String> roles;
    List<String> users;
    boolean enabled = true;

    public RoleMapping(@NonNull String mappingName) {
        this.mappingName = mappingName;
    }

    // Getter-Methods
    public @NonNull String getMappingName() { return mappingName; }
    public List<String> getRoles() { return roles; }
    public List<String> getUsers() { return users; }
    public boolean isEnabled() { return enabled; }

    // Setter-Methods
    public void setMappingName(@NonNull String mappingName) { this.mappingName = mappingName; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setUsers(List<String> users) { this.users = users; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
