package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.List;

public class Role {
    String name;
    List<Application>  applications;
    List<String> cluster;
    List<Index> indices;

    // Getter-Methods
    public String getName() { return name; }

    public List<Application> getApplications() { return applications; }

    public List<String> getCluster() { return cluster; }

    public List<Index> getIndices() { return indices; }

    // Setter-Methods
    public void setName(String name) { this.name = name; }

    public void setApplications(List<Application> applications) { this.applications = applications; }

    public void setCluster(List<String> cluster) { this.cluster = cluster; }

    public void setIndices(List<Index> indices) { this.indices = indices; }

    public static class Application {
        String name;
        List<String> privileges;
        List<String> resources;

        // Getter-Methods
        public String getName() {
            return name;
        }
        public List<String> getPrivileges() {
            return privileges;
        }
        public List<String> getResources() {
            return resources;
        }

        // Setter-Methods
        public void  setName(String name) {
            this.name = name;
        }
        public void  setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }
        public void  setResources(List<String> resources) {
            this.resources = resources;
        }

        @Override
        public String toString() {
            return "Application [name=" + name + ", privileges=" + privileges + ", resources=" + resources + "]";
        }
    }

    public static class Index {
        FieldSecurity fieldSecurity;
        List<String> names;
        List<String> privileges;
        // TODO: Add query property
        boolean allowRestrictedIndices;
        // Getter-Methods
        public FieldSecurity getFieldSecurity() { return fieldSecurity; }
        public List<String> getNames() { return names; }
        public List<String> getPrivileges() { return privileges; }
        public boolean isAllowRestrictedIndices() { return allowRestrictedIndices; }

        // Setter-Methods
        public void setFieldSecurity(FieldSecurity fieldSecurity) { this.fieldSecurity = fieldSecurity; }
        public void setNames(List<String> names) { this.names = names; }
        public void setPrivileges(List<String> privileges) { this.privileges = privileges; }
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
