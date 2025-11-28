package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class RoleMapping {
    @NonNull String mappingName;
    List<String> roles;
    boolean enabled = true;
    List<String> runAs;

    Rules rules;
    Metadata metadata;
    List<RoleTemplate> roleTemplates;

    public RoleMapping(@NonNull String mappingName) {
        this.mappingName = mappingName;
    }

    // Getter-Methods
    public @NonNull String getMappingName() { return mappingName; }
    public List<String> getRoles() { return roles; }
    public boolean isEnabled() { return enabled; }
    public List<String> getRunAs() { return runAs; }
    public Rules getRules() { return rules; }
    public Metadata getMetadata() { return metadata; }
    public List<RoleTemplate> getRoleTemplates() { return roleTemplates; }

    // Setter-Methods
    public void setMappingName(@NonNull String mappingName) { this.mappingName = mappingName; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRunAs(List<String> runAS) { this.runAs = runAS; }
    public void setRules(Rules rules) { this.rules = rules; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
    public void setRoleTemplates(List<RoleTemplate> roleTemplates) { this.roleTemplates = roleTemplates; }


    public static class Rules {
        Map<String, Object> field;
        List<Rules> any;
        List<Rules> all;
        Rules except;

        // Getter
        public Map<String, Object> getField() { return field; }
        public List<Rules> getAny() { return any; }
        public List<Rules> getAll() { return all; }
        public Rules getExcept() { return except; }

        // Setter
        public void setField(Map<String, Object> field) { this.field = field; }
        public void setAny(List<Rules> any) { this.any = any; }
        public void setAll(List<Rules> all) { this.all = all; }
        public void setExcept(Rules except) { this.except = except; }

        @Override
        public String toString() {
            return "Rules[field=" + field +
                    ", any=" + any +
                    ", all=" + all +
                    ", except=" + except + "]";
        }
    }

    public static class Metadata {
        Map<String, Object> entries;

        public Map<String, Object> getEntries() { return entries; }
        public void setEntries(Map<String, Object> entries) { this.entries = entries; }

        @Override
        public String toString() {
            return "Metadata[entries=" + entries + "]";
        }
    }

    public static class RoleTemplate {

        public enum Format {
            STRING,
            JSON;

            public static Format fromString(String value) {
                if (value == null) {
                    return STRING;
                }
                return switch (value.toLowerCase()) {
                    case "string" -> STRING;
                    case "json" -> JSON;
                    default -> null;
                };
            }
        }

        Format format = Format.STRING;
        String template;

        public Format getFormat() { return format; }
        public String getTemplate() { return template; }

        public void setFormat(Format format) { this.format = format; }
        public void setTemplate(String template) { this.template = template; }

        @Override
        public String toString() {
            return "RoleTemplate[format=" + format + ", template=" + template +"]";
        }
    }



    @Override
    public String toString() {
        return "RoleMapping[" +
                "\n\tmappingName=" + mappingName +
                "\n\troles=" + roles +
                "\n\tenabled=" + enabled +
                "\n\trunAs=" + runAs +
                "\n\trules=" + rules +
                "\n\tmetadata=" + metadata +
                "\n\troleTemplates=" + roleTemplates +
                "\n]";
    }
}
