package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoleMapping {
    @NonNull private String mappingName;
    private final List<String> roles = new ArrayList<>();
    private final List<String> rolesView = Collections.unmodifiableList(roles);
    private boolean rolesSet;

    private final List<String> users = new ArrayList<>();
    private final List<String> usersView = Collections.unmodifiableList(users);
    private boolean usersSet;
    private boolean enabled = true;
    private final List<String> runAs = new ArrayList<>();
    private final List<String> runAsView = Collections.unmodifiableList(runAs);
    private boolean runAsSet;

    private Rules rules;
    private Metadata metadata;
    private final List<RoleTemplate> roleTemplates = new ArrayList<>();
    private final List<RoleTemplate> roleTemplatesView = Collections.unmodifiableList(roleTemplates);
    private boolean roleTemplatesSet;

    public RoleMapping(@NonNull String mappingName) {
        this.mappingName = mappingName;
    }

    // Getter-Methods
    public @NonNull String getMappingName() { return mappingName; }
    public List<String> getRoles() { return rolesSet ? rolesView : null; }
    public List<String> getUsers() { return usersSet ? usersView : null; }
    public boolean isEnabled() { return enabled; }
    public List<String> getRunAs() { return runAsSet ? runAsView : null; }
    public Rules getRules() { return rules; }
    public Metadata getMetadata() { return metadata; }
    public List<RoleTemplate> getRoleTemplates() { return roleTemplatesSet ? roleTemplatesView : null; }

    // Setter-Methods
    public void setMappingName(@NonNull String mappingName) { this.mappingName = mappingName; }
    public void setRoles(List<String> roles) { replaceStrings(roles, this.roles, () -> rolesSet = true, () -> rolesSet = false); }
    public void setUsers(List<String> users) { replaceStrings(users, this.users, () -> usersSet = true, () -> usersSet = false); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRunAs(List<String> runAS) { replaceStrings(runAS, this.runAs, () -> runAsSet = true, () -> runAsSet = false); }
    public void setRules(Rules rules) { this.rules = rules == null ? null : rules.freeze(); }
    public void setMetadata(Metadata metadata) { this.metadata = metadata == null ? null : metadata.freeze(); }
    public void setRoleTemplates(List<RoleTemplate> roleTemplates) {
        if (roleTemplates == null) {
            this.roleTemplates.clear();
            roleTemplatesSet = false;
            return;
        }
        roleTemplatesSet = true;
        this.roleTemplates.clear();
        for (RoleTemplate template : roleTemplates) {
            this.roleTemplates.add(template == null ? null : template.freeze());
        }
    }


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

    private static void replaceStrings(List<String> source, List<String> target, Runnable setFlag, Runnable unsetFlag) {
        if (source == null) {
            target.clear();
            unsetFlag.run();
            return;
        }
        setFlag.run();
        target.clear();
        target.addAll(source);
    }
}
