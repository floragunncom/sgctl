package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Role {
    @NonNull private String name;
    private List<Application>  applications = List.of();
    private List<String> cluster = List.of();
    private List<RemoteCluster> remoteClusters = List.of();
    private List<Index> indices = List.of();
    private List<RemoteIndex> remoteIndices = List.of();
    private List<String> runAs = List.of();
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
    public void setApplications(List<Application> applications) { this.applications = freezeList(applications); }
    public void setCluster(List<String> cluster) { this.cluster = freezeList(cluster); }
    public void setIndices(List<Index> indices) { this.indices = freezeList(indices); }
    public void setRunAs(List<String> runAs) { this.runAs = freezeList(runAs); }
    public void setDescription(String description) { this.description = description; }
    public void setRemoteIndices(List<RemoteIndex> remoteIndices) { this.remoteIndices = freezeList(remoteIndices); }
    public void setRemoteClusters(List<RemoteCluster> remoteClusters) { this.remoteClusters = freezeList(remoteClusters); }

    public static class Application {
        @NonNull private String name;
        @NonNull private List<String> privileges;
        @NonNull private List<String> resources;

        public Application(@NonNull String name, @NonNull List<String> privileges, @NonNull List<String> resources) {
            this.name = name;
            this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges)));
            this.resources = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(resources)));
        }

        // Getter-Methods
        public @NonNull String getName() {
            return name;
        }
        public @NonNull List<String> getPrivileges() { return privileges; }
        public @NonNull List<String> getResources() { return resources; }

        // Setter-Methods
        public void  setName(@NonNull String name) {
            this.name = name;
        }
        public void  setPrivileges(@NonNull List<String> privileges) {
            this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges)));
        }
        public void  setResources(@NonNull List<String> resources) {
            this.resources = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(resources)));
        }

        @Override
        public String toString() {
            return "Application [name=" + name + ", privileges=" + privileges + ", resources=" + resources + "]";
        }
    }

    public static class RemoteCluster {
        private List<String> clusters = List.of();
        private List<String> privileges = List.of();

        // Getter-Methods
        public List<String> getClusters() { return clusters; }
        public List<String> getPrivileges() { return privileges; }

        // Setter-Methods
        public void setClusters(List<String> clusters) { this.clusters = freezeList(clusters); }
        public void setPrivileges(List<String> privileges) { this.privileges = freezeList(privileges); }

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
            this.names = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(names)));
            this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges)));
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
        public void setNames(@NonNull List<String> names) { this.names = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(names))); }
        public void setPrivileges(@NonNull List<String> privileges) { this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges))); }
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
            this.cluster = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cluster)));
        }

        public @NonNull List<String> getCluster() { return cluster; }

        public void setCluster(@NonNull List<String> cluster) { this.cluster = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cluster))); }

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
        private List<String> except = List.of();
        private List<String> grant = List.of();

        // Getter-Methods
        public List<String> getExcept() { return except; }
        public List<String> getGrant() { return grant; }

        // Setter-methods
        public void setExcept(List<String> except) { this.except = freezeList(except); }
        public void setGrant(List<String> grant) { this.grant = freezeList(grant); }

        @Override
        public String toString() {
            return "FieldSecurity [except=" + except + ", grant=" + grant + "]";
        }
    }

    @Override
    public String toString() {
        return "Role[\n\tname=" + name + "\n\tapplications=" + applications + "\n\tcluster=" + cluster + "\n\tremoteCluster=" + remoteClusters + "\n\tindices=" + indices + "\n\tremote_indices=" + remoteIndices + "\n]";
    }

    private static <T> List<T> freezeList(List<T> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
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
