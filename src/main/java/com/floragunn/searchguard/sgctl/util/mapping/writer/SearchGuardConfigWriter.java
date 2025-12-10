package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

public class SearchGuardConfigWriter {
    MigrateConfig.SgAuthc sg_authc;
    UserConfigWriter userConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;


    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        sg_authc = new MigrateConfig.SgAuthc();
        userConfig = new UserConfigWriter(ir);
        roleConfig = new RoleConfigWriter(ir, sg_authc);
        mappingWriter = new RoleMappingWriter(ir);
    }
}
