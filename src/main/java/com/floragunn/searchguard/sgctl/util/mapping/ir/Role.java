package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.List;

public class Role {
    String name;
    List<Application>  applications;
    List<String> cluster;

    // Getter-Methods
    public String getName() {
        return name;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public List<String> getCluster() {
        return cluster;
    }

    // Setter-Methods
    public void setName(String name) {
        this.name = name;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public void setCluster(List<String> cluster) {
        this.cluster = cluster;
    }

    public static class Application {
        String name;
        List<String> privileges;
        List<String> resources;

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
    }
}
