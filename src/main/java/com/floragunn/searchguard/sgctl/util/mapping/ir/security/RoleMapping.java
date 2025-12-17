package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoleMapping {
    @NonNull private String mappingName;
    private List<String> roles;
    private List<String> users;
    private boolean enabled = true;
    private List<String> runAs;

    private Rules rules;
    private Metadata metadata;
    private List<RoleTemplate> roleTemplates;

    public RoleMapping(@NonNull String mappingName) {
        this.mappingName = mappingName;
    }

    // Getter-Methods
    public @NonNull String getMappingName() { return mappingName; }
    public List<String> getRoles() { return roles; }
    public List<String> getUsers() { return users; }
    public boolean isEnabled() { return enabled; }
    public List<String> getRunAs() { return runAs; }
    public Rules getRules() { return rules; }
    public Metadata getMetadata() { return metadata; }
    public List<RoleTemplate> getRoleTemplates() { return roleTemplates; }

    // Setter-Methods
    public void setMappingName(@NonNull String mappingName) { this.mappingName = mappingName; }
    public void setRoles(List<String> roles) { this.roles = freezeList(roles); }
    public void setUsers(List<String> users) { this.users = freezeList(users); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRunAs(List<String> runAS) { this.runAs = freezeList(runAS); }
    public void setRules(Rules rules) { this.rules = rules == null ? null : rules.freeze(); }
    public void setMetadata(Metadata metadata) { this.metadata = metadata == null ? null : metadata.freeze(); }
    public void setRoleTemplates(List<RoleTemplate> roleTemplates) { this.roleTemplates = freezeRoleTemplates(roleTemplates); }


    public static class Rules {
        private Map<String, Object> field = Map.of();
        private List<Rules> any = List.of();
        private List<Rules> all = List.of();
        private Rules except;
        private boolean frozen;

        // Getter
        public Map<String, Object> getField() { return field; }
        public List<Rules> getAny() { return any; }
        public List<Rules> getAll() { return all; }
        public Rules getExcept() { return except; }

        // Setter
        public void setField(Map<String, Object> field) { ensureMutable(); this.field = freezeMap(field); }
        public void setAny(List<Rules> any) { ensureMutable(); this.any = freezeList(any); }
        public void setAll(List<Rules> all) { ensureMutable(); this.all = freezeList(all); }
        public void setExcept(Rules except) { ensureMutable(); this.except = except == null ? null : except.freeze(); }

        private Rules freeze() {
            var copy = new Rules();
            copy.field = this.field;
            copy.any = freezeNestedRules(any);
            copy.all = freezeNestedRules(all);
            copy.except = this.except == null ? null : this.except.freeze();
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("Rules is frozen");
            }
        }

        @Override
        public String toString() {
            return "Rules[field=" + field +
                    ", any=" + any +
                    ", all=" + all +
                    ", except=" + except + "]";
        }

        private static List<Rules> freezeNestedRules(List<Rules> rules) {
            if (rules == null || rules.isEmpty()) {
                return List.of();
            }
            List<Rules> copy = new ArrayList<>(rules.size());
            for (Rules rule : rules) {
                copy.add(rule == null ? null : rule.freeze());
            }
            return Collections.unmodifiableList(copy);
        }
    }

    public static class Metadata {
        private Map<String, Object> entries = Map.of();
        private boolean frozen;

        public Map<String, Object> getEntries() { return entries; }
        public void setEntries(Map<String, Object> entries) { ensureMutable(); this.entries = freezeMap(entries); }

        private Metadata freeze() {
            var copy = new Metadata();
            copy.entries = this.entries;
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("Metadata is frozen");
            }
        }

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

        private Format format = Format.STRING;
        private String template;
        private boolean frozen;

        public Format getFormat() { return format; }
        public String getTemplate() { return template; }

        public void setFormat(Format format) { ensureMutable(); this.format = format; }
        public void setTemplate(String template) { ensureMutable(); this.template = template; }

        private RoleTemplate freeze() {
            var copy = new RoleTemplate();
            copy.format = this.format;
            copy.template = this.template;
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("RoleTemplate is frozen");
            }
        }

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
                "\n\tusers=" + users +
                "\n\tenabled=" + enabled +
                "\n\trunAs=" + runAs +
                "\n\trules=" + rules +
                "\n\tmetadata=" + metadata +
                "\n\troleTemplates=" + roleTemplates +
                "\n]";
    }

    private static <T> List<T> freezeList(List<T> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static <K, V> Map<K, V> freezeMap(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static List<RoleTemplate> freezeRoleTemplates(List<RoleTemplate> roleTemplates) {
        if (roleTemplates == null) {
            return null;
        }
        if (roleTemplates.isEmpty()) {
            return List.of();
        }

        List<RoleTemplate> copy = new ArrayList<>(roleTemplates.size());
        for (RoleTemplate template : roleTemplates) {
            copy.add(template == null ? null : template.freeze());
        }
        return Collections.unmodifiableList(copy);
    }
}
