package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.List;

public class Role {
    String name;
    List<Application>  applications;
    List<String> cluster;

    public static class Application {
        String name;
        List<String> privileges;
        List<String> resources;

        public void  setName(String name) {
            this.name = name;
        }
        public void  setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }
        public void  setResources(List<String> resources) {
            this.resources = resources;
        }
    }
}
