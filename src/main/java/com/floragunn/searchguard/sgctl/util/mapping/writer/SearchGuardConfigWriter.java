package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;

/**
 * Top-level configuration writer for Search Guard.
 * Generates sg_authc.yml with LDAP, SAML, OIDC realms mapped.
 */
public class SearchGuardConfigWriter {
    MigrateConfig.SgAuthc  sg_authc;
    MigrateConfig.SgAuthc sg_frontend_authc;

    public SearchGuardConfigWriter(IntermediateRepresentationElasticSearchYml irElasticSearchYml, IntermediateRepresentation ir) {
        //If there is a specific reason why we can't just make one initial call please tell me.
        SGAuthcTranslator.Configs configs = SGAuthcTranslator.createAuthcConfig(irElasticSearchYml);
        sg_authc = configs.config;
        sg_frontend_authc = configs.fconfig;
    }

    public MigrateConfig.SgAuthc getSg_frontend_authc() {
        return sg_frontend_authc;
    }
}
