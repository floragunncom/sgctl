package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.List;

public class RoleConfigWriter implements Document<RoleConfigWriter> {
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGRole> roles;

    public RoleConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
    }

    public void createSGRoles() {

    }

    @Override
    public Object toBasicObject() {
        return null;
    }

    static class SGRole implements Document<SGRole> {

        @Override
        public Object toBasicObject() {
            return null;
        }
    }
}
