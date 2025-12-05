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

        @Override
        public Object toBasicObject() {
            return null;
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
