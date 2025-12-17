package com.floragunn.searchguard.sgctl.util.mapping.ir.security;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
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
    public void setApplications(List<Application> applications) { this.applications = freezeApplications(applications); }
    public void setCluster(List<String> cluster) { this.cluster = freezeList(cluster); }
    public void setIndices(List<Index> indices) { this.indices = freezeIndices(indices); }
    public void setRunAs(List<String> runAs) { this.runAs = freezeList(runAs); }
    public void setDescription(String description) { this.description = description; }
    public void setRemoteIndices(List<RemoteIndex> remoteIndices) { this.remoteIndices = freezeRemoteIndices(remoteIndices); }
    public void setRemoteClusters(List<RemoteCluster> remoteClusters) { this.remoteClusters = freezeRemoteClusters(remoteClusters); }

    public static class Application {
        @NonNull private String name;
        @NonNull private List<String> privileges;
        @NonNull private List<String> resources;
        private boolean frozen;

        public Application(@NonNull String name, @NonNull List<String> privileges, @NonNull List<String> resources) {
            this.name = name;
            this.privileges = freezeStringList(privileges);
            this.resources = freezeStringList(resources);
        }

        // Getter-Methods
        public @NonNull String getName() {
            return name;
        }
        public @NonNull List<String> getPrivileges() { return privileges; }
        public @NonNull List<String> getResources() { return resources; }

        // Setter-Methods
        public void  setName(@NonNull String name) {
            ensureMutable();
            this.name = name;
        }
        public void  setPrivileges(@NonNull List<String> privileges) {
            ensureMutable();
            this.privileges = freezeStringList(privileges);
        }
        public void  setResources(@NonNull List<String> resources) {
            ensureMutable();
            this.resources = freezeStringList(resources);
        }

        Application freeze() {
            var copy = new Application(name, privileges, resources);
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("Application is frozen");
            }
        }

        @Override
        public String toString() {
            return "Application [name=" + name + ", privileges=" + privileges + ", resources=" + resources + "]";
        }
    }

    public static class RemoteCluster {
        private List<String> clusters = List.of();
        private List<String> privileges = List.of();
        private boolean frozen;

        // Getter-Methods
        public List<String> getClusters() { return clusters; }
        public List<String> getPrivileges() { return privileges; }

        // Setter-Methods
        public void setClusters(List<String> clusters) { ensureMutable(); this.clusters = freezeList(clusters); }
        public void setPrivileges(List<String> privileges) { ensureMutable(); this.privileges = freezeList(privileges); }

        RemoteCluster freeze() {
            var copy = new RemoteCluster();
            copy.clusters = this.clusters;
            copy.privileges = this.privileges;
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("RemoteCluster is frozen");
            }
        }

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
        protected boolean frozen;

        public Index(@NonNull List<String> names, @NonNull List<String> privileges, FieldSecurity fieldSecurity, String query, Boolean allowRestrictedIndices) {
            this.names = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(names)));
            this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges)));
            this.fieldSecurity = fieldSecurity == null ? null : fieldSecurity.freeze();
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
        public void setNames(@NonNull List<String> names) { ensureMutable(); this.names = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(names))); }
        public void setPrivileges(@NonNull List<String> privileges) { ensureMutable(); this.privileges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(privileges))); }
        public void setFieldSecurity(FieldSecurity fieldSecurity) { ensureMutable(); this.fieldSecurity = fieldSecurity == null ? null : fieldSecurity.freeze(); }
        public void setQuery(String query) { ensureMutable(); this.query = query; }
        public void setAllowRestrictedIndices(Boolean allowRestrictedIndices) { ensureMutable(); this.allowRestrictedIndices = allowRestrictedIndices; }

        Index freeze() {
            var copy = new Index(names, privileges, fieldSecurity == null ? null : fieldSecurity.freeze(), query, allowRestrictedIndices);
            copy.frozen = true;
            return copy;
        }

        protected void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("Index is frozen");
            }
        }

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

        public void setCluster(@NonNull List<String> cluster) { ensureMutable(); this.cluster = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cluster))); }

        @Override
        RemoteIndex freeze() {
            var copy = new RemoteIndex(cluster, getNames(), getPrivileges(), getFieldSecurity() == null ? null : getFieldSecurity().freeze(), getQuery(), isAllowRestrictedIndices());
            copy.frozen = true;
            return copy;
        }

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
        private boolean frozen;

        // Getter-Methods
        public List<String> getExcept() { return except; }
        public List<String> getGrant() { return grant; }

        // Setter-methods
        public void setExcept(List<String> except) { ensureMutable(); this.except = freezeList(except); }
        public void setGrant(List<String> grant) { ensureMutable(); this.grant = freezeList(grant); }

        FieldSecurity freeze() {
            var copy = new FieldSecurity();
            copy.except = this.except;
            copy.grant = this.grant;
            copy.frozen = true;
            return copy;
        }

        private void ensureMutable() {
            if (frozen) {
                throw new IllegalStateException("FieldSecurity is frozen");
            }
        }

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
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static List<Application> freezeApplications(List<Application> source) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return List.of();
        }
        List<Application> frozen = new ArrayList<>(source.size());
        for (Application application : source) {
            frozen.add(application == null ? null : application.freeze());
        }
        return Collections.unmodifiableList(frozen);
    }

    private static List<RemoteCluster> freezeRemoteClusters(List<RemoteCluster> source) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return List.of();
        }
        List<RemoteCluster> frozen = new ArrayList<>(source.size());
        for (RemoteCluster remoteCluster : source) {
            frozen.add(remoteCluster == null ? null : remoteCluster.freeze());
        }
        return Collections.unmodifiableList(frozen);
    }

    private static List<Index> freezeIndices(List<Index> source) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return List.of();
        }
        List<Index> frozen = new ArrayList<>(source.size());
        for (Index index : source) {
            frozen.add(index == null ? null : index.freeze());
        }
        return Collections.unmodifiableList(frozen);
    }

    private static List<RemoteIndex> freezeRemoteIndices(List<RemoteIndex> source) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return List.of();
        }
        List<RemoteIndex> frozen = new ArrayList<>(source.size());
        for (RemoteIndex index : source) {
            frozen.add(index == null ? null : index.freeze());
        }
        return Collections.unmodifiableList(frozen);
    }

    private static List<String> freezeStringList(List<String> source) {
        Objects.requireNonNull(source);
        if (source.isEmpty()) {
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
