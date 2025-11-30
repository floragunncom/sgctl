package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

public class SearchGuardConfigWriter {
    IntermediateRepresentation ir;
    UserConfigWriter userConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;


    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        userConfig = new UserConfigWriter(ir);
        roleConfig = new RoleConfigWriter(ir);
        mappingWriter = new RoleMappingWriter(ir);
    }
}
