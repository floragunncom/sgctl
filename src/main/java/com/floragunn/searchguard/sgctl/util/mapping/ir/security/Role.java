package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

public class Role {
    @NonNull private String name;
    private List<Application>  applications;
    private List<String> cluster;
    private List<RemoteCluster> remoteClusters;
    private List<Index> indices;
    private List<RemoteIndex> remoteIndices;
    private List<String> runAs;
    private String description;

    public Role(@NonNull String name) {
        this.name = name;
    }

    // Getter-Methods
    public @NonNull String getName() { return name; }
    public List<Application> getApplications() { return applications; }
    public List<String> getCluster() { return cluster; }
    public List<Index> getIndices() { return indices; }
    public List<String> getRunAs() { return runAs; }
    public String getDescription() { return description; }
    public List<RemoteIndex> getRemoteIndices() { return remoteIndices; }
    public List<RemoteCluster> getRemoteClusters() { return remoteClusters; }

    // Setter-Methods
    public void setName(@NonNull String name) { this.name = name; }
    public void setApplications(List<Application> applications) { this.applications = applications; }
    public void setCluster(List<String> cluster) { this.cluster = cluster; }
    public void setIndices(List<Index> indices) { this.indices = indices; }
    public void setRunAs(List<String> runAs) { this.runAs = runAs; }
    public void setDescription(String description) { this.description = description; }
    public void setRemoteIndices(List<RemoteIndex> remoteIndices) { this.remoteIndices = remoteIndices; }
    public void setRemoteClusters(List<RemoteCluster> remoteClusters) { this.remoteClusters = remoteClusters; }

    public static class Application {
        @NonNull private String name;
        @NonNull private List<String> privileges;
        @NonNull private List<String> resources;

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

    public static class RemoteCluster {
        private List<String> clusters;
        private List<String> privileges;

        // Getter-Methods
        public List<String> getClusters() { return clusters; }
        public List<String> getPrivileges() { return privileges; }

        // Setter-Methods
        public void setClusters(List<String> clusters) { this.clusters = clusters; }
        public void setPrivileges(List<String> privileges) { this.privileges = privileges; }

        @Override
        public String toString() {
            return "Remote Cluster [clusters=" + clusters + ", privileges=" + privileges + "]";
        }
    }

    public static class Index {
        /// Only set when this index is a remote index
        @NonNull private List<String> names;
        @NonNull private List<String> privileges;
        private FieldSecurity fieldSecurity;
        private String query;
        // TODO: Add query property
        private Boolean allowRestrictedIndices;

        public Index(@NonNull List<String> names, @NonNull List<String> privileges, FieldSecurity fieldSecurity, String query, Boolean allowRestrictedIndices) {
            this.names = names;
            this.privileges = privileges;
            this.fieldSecurity = fieldSecurity;
            this.query = query;
            this.allowRestrictedIndices = allowRestrictedIndices;
        }

        // Getter-Methods
        public @NonNull List<String> getNames() { return names; }
        public @NonNull List<String> getPrivileges() { return privileges; }
        public FieldSecurity getFieldSecurity() { return fieldSecurity; }
        public String getQuery() { return query; }
        public Boolean isAllowRestrictedIndices() { return allowRestrictedIndices; }

        // Setter-Methods
        public void setNames(@NonNull List<String> names) { this.names = names; }
        public void setPrivileges(@NonNull List<String> privileges) { this.privileges = privileges; }
        public void setFieldSecurity(FieldSecurity fieldSecurity) { this.fieldSecurity = fieldSecurity; }
        public void setQuery(String query) { this.query = query; }
        public void setAllowRestrictedIndices(Boolean allowRestrictedIndices) { this.allowRestrictedIndices = allowRestrictedIndices; }

        @Override
        public String toString() {
            return "Index [fieldSecurity=" + fieldSecurity + ", names=" + names + ", privileges=" + privileges + ", allowRestrictedIndices=" + allowRestrictedIndices + "]";
        }
    }

    public static class RemoteIndex extends Index {
        @NonNull private List<String> cluster;

        public RemoteIndex(@NonNull List<String> cluster, @NonNull List<String> names, @NonNull List<String> privileges, FieldSecurity fieldSecurity, String query, Boolean allowRestrictedIndices) {
            super(names, privileges, fieldSecurity, query, allowRestrictedIndices);
            this.cluster = cluster;
        }

        public @NonNull List<String> getCluster() { return cluster; }

        public void setCluster(@NonNull List<String> cluster) { this.cluster = cluster; }

        @Override
        public String toString() {
            return "Remote Index [cluster=" + cluster
                    + ", fieldSecurity=" + getFieldSecurity()
                    + ", names=" + getNames()
                    + ", privileges=" + getPrivileges()
                    + ", allowRestrictedIndices=" + isAllowRestrictedIndices()
                    + "]";
        }
    }

    public static class FieldSecurity {
        private List<String> except;
        private List<String> grant;

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
        return "Role[\n\tname=" + name + "\n\tapplications=" + applications + "\n\tcluster=" + cluster + "\n\tremoteCluster=" + remoteClusters + "\n\tindices=" + indices + "\n\tremote_indices=" + remoteIndices + "\n]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
