package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

public class SearchGuardConfigWriter {
    MigrateConfig.SgAuthc sg_authc;
    UserConfigWriter userConfig;
    ActionGroupConfigWriter actionGroupConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;


    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        sg_authc = new MigrateConfig.SgAuthc();
        userConfig = new UserConfigWriter(ir);
        actionGroupConfig = new ActionGroupConfigWriter(ir);
        roleConfig = new RoleConfigWriter(ir, sg_authc, actionGroupConfig);
        mappingWriter = new RoleMappingWriter(ir);
    }
}
