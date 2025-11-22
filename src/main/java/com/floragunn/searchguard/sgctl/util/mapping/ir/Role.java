package com.floragunn.searchguard.sgctl.util.mapping.ir;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class Role {
    @NonNull String name;
    List<Application>  applications;
    List<String> cluster;
    List<Index> indices;

    public Role(@NonNull String name) {
        this.name = name;
    }

    // Getter-Methods
    public @NonNull String getName() { return name; }

    public List<Application> getApplications() { return applications; }

    public List<String> getCluster() { return cluster; }

    public List<Index> getIndices() { return indices; }

    // Setter-Methods
    public void setName(@NonNull String name) { this.name = name; }

    public void setApplications(List<Application> applications) { this.applications = applications; }

    public void setCluster(List<String> cluster) { this.cluster = cluster; }

    public void setIndices(List<Index> indices) { this.indices = indices; }

    public static class Application {
        @NonNull String name;
        @NonNull List<String> privileges;
        @NonNull List<String> resources;

        public Application(@NonNull String name, @NonNull List<String> privileges, @NonNull List<String> resources) {
            this.name = name;
            this.privileges = privileges;
            this.resources = resources;
        }

        // Getter-Methods
        public @NonNull String getName() {
            return name;
        }
        public @NonNull List<String> getPrivileges() {
            return privileges;
        }
        public @NonNull List<String> getResources() {
            return resources;
        }

        // Setter-Methods
        public void  setName(@NonNull String name) {
            this.name = name;
        }
        public void  setPrivileges(@NonNull List<String> privileges) {
            this.privileges = privileges;
        }
        public void  setResources(@NonNull List<String> resources) {
            this.resources = resources;
        }

        @Override
        public String toString() {
            return "Application [name=" + name + ", privileges=" + privileges + ", resources=" + resources + "]";
        }
    }

    public static class Index {
        @NonNull List<String> names = List.of();
        @NonNull List<String> privileges = List.of();
        FieldSecurity fieldSecurity;
        // TODO: Add query property
        boolean allowRestrictedIndices;

        // Getter-Methods
        public FieldSecurity getFieldSecurity() { return fieldSecurity; }
        public @NonNull List<String> getNames() { return names; }
        public @NonNull List<String> getPrivileges() { return privileges; }
        public boolean isAllowRestrictedIndices() { return allowRestrictedIndices; }

        // Setter-Methods
        public void setFieldSecurity(FieldSecurity fieldSecurity) { this.fieldSecurity = fieldSecurity; }
        public void setNames(@NonNull List<String> names) { this.names = names; }
        public void setPrivileges(@NonNull List<String> privileges) { this.privileges = privileges; }
        public void setAllowRestrictedIndices(boolean allowRestrictedIndices) { this.allowRestrictedIndices = allowRestrictedIndices; }

        @Override
        public String toString() {
            return "Index [fieldSecurity=" + fieldSecurity + ", names=" + names + ", privileges=" + privileges + ", allowRestrictedIndices=" + allowRestrictedIndices + "]";
        }
    }

    public static class FieldSecurity {
        List<String> except;
        List<String> grant;

        // Getter-Methods
        public List<String> getExcept() { return except; }
        public List<String> getGrant() { return grant; }

        // Setter-methods
        public void setExcept(List<String> except) { this.except = except; }
        public void setGrant(List<String> grant) { this.grant = grant; }

        @Override
        public String toString() {
            return "FieldSecurity [except=" + except + ", grant=" + grant + "]";
        }
    }

    @Override
    public String toString() {
        return "Role[\n\tname=" + name + "\n\tapplications=" + applications + "\n\tcluster=" + cluster + "\n\tindices=" + indices + "\n]";
    }
}
