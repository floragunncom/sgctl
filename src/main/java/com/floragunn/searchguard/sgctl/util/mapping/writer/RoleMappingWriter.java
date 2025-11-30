package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.List;

public class RoleMappingWriter implements Document<RoleMappingWriter>{
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGRoleMapping> roles;

    public RoleMappingWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
    }

    public void createSGRoleMappings() {

    }

    @Override
    public Object toBasicObject() {
        return null;
    }

    static class SGRoleMapping implements Document<SGRoleMapping> {

        @Override
        public Object toBasicObject() {
            return null;
        }
    }
}
