package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class RoleMappingWriter implements Document<RoleMappingWriter>{
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGRoleMapping> roles;

    private static final String FILE_NAME = "sg_roles_mapping.yml";

    public RoleMappingWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        createSGRoleMappings();
        print(DocWriter.yaml().writeAsString(this));
    }

    public void createSGRoleMappings() {
        //TODO: implement
    }

    @Override
    public Object toBasicObject() {
        Map<String, SGRoleMapping> contents = new LinkedHashMap<>();
        //TODO: implement
        return contents;
    }

    static class SGRoleMapping implements Document<SGRoleMapping> {
        String mappingName;
        List<String> users = new ArrayList<>();
        List<String> backendRoles = new ArrayList<>();
        List<String> hosts = new ArrayList<>();
        List<String> ips = new ArrayList<>();

        SGRoleMapping(String mappingName, List<String> users, List<String> backendRoles, List<String> hosts, List<String> ips) {
            this.mappingName = mappingName;
            this.users = users;
            this.backendRoles = backendRoles;
            this.hosts = hosts;
            this.ips = ips;
        }

        @Override
        public Object toBasicObject() {
            var contents = new LinkedHashMap<String, Object>();

            if (users.isEmpty()) {
                contents.put("users", users);
            }

            if (backendRoles.isEmpty()) {
                contents.put("backend_roles", backendRoles);
            }

            if (hosts.isEmpty()) {
                contents.put("hosts", hosts);
            }

            if (ips.isEmpty()) {
                contents.put("ips", ips);
            }

            return contents;
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
