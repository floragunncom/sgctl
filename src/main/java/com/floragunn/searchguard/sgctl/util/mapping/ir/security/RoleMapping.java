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
    public List<String> getRunAS() { return runAs; }
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
        List<Map<String, Object>> any;
        List<Map<String, Object>> all;
        Map<String, Object> except;

        public Map<String, Object> getField() { return field; }
        public List<Map<String, Object>> getAny() { return any; }
        public List<Map<String, Object>> getAll() { return all; }
        public Map<String, Object> getExcept() { return except; }

        public void setField(Map<String, Object> field) { this.field = field; }
        public void setAny(List<Map<String, Object>> any) { this.any = any; }
        public void setAll(List<Map<String, Object>> all) { this.all = all; }
        public void setExcept(Map<String, Object> except) { this.except = except; }

        @Override
        public String toString() {
            return "Rules[field=" + field + ", any=" + any + ", all=" + all +
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
        String format;
        Template template;

        public String getFormat() { return format; }
        public Template getTemplate() { return template; }

        public void setFormat(String format) { this.format = format; }
        public void setTemplate(Template template) { this.template = template; }

        @Override
        public String toString() {
            return "RoleTemplate[format=" + format + ", template=" + template +"]";
        }
    }

    public static class Template {
        Object source;
        String id;
        Map<String, Object> params;
        String lang;
        Map<String, Object> options;

        public Object getSource() { return source; }
        public String getId() { return id; }
        public Map<String, Object> getParams() { return params; }
        public String getLang() { return lang; }
        public Map<String, Object> getOptions() { return options; }

        public void setSource(Object source) { this.source = source; }
        public void setId(String id) { this.id = id; }
        public void setParams(Map<String, Object> params) { this.params = params; }
        public void setLang(String lang) { this.lang = lang; }
        public void setOptions(Map<String, Object> options) { this.options = options; }

        @Override
        public String toString() {
            return "Template[source=" + source + ", id=" + id + ", params=" + params +
                    ", lang=" + lang + ", options=" + options + "]";
        }
    }

    @Override
    public String toString() {
        return "RoleMapping[" +
                "mappingName=" + mappingName +
                ", roles=" + roles +
                ", enabled=" + enabled +
                ", runAs=" + runAs +
                ", rules=" + rules +
                ", metadata=" + metadata +
                ", roleTemplates=" + roleTemplates +
                "]";
    }
}
