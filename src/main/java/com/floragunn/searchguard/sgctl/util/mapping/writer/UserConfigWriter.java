package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.List;

public class UserConfigWriter implements Document<UserConfigWriter> {
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGInternalUser> users;

    public UserConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        createSGInternalUser();
    }

    private void createSGInternalUser() {

    }

    @Override
    public Object toBasicObject() {
        return users;
    }

    static class SGInternalUser implements Document<SGInternalUser> {

        @Override
        public Object toBasicObject() {

            return null;
        }
    }
}
