package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;

/**
 * Top-level configuration writer for Search Guard.
 * Generates sg_authc.yml with LDAP, SAML, OIDC realms mapped.
 */
public class SearchGuardConfigWriter {
    SGAuthcTranslator authcTranslator;
    MigrateConfig.SgAuthc  sgAuthc;
    MigrateConfig.SgAuthc sg_frontend_authc;

    public SearchGuardConfigWriter(IntermediateRepresentationElasticSearchYml irElasticSearchYml, IntermediateRepresentation ir) {
        //If there is a specific reason why we can't just make one initial call please tell me.
        authcTranslator = new SGAuthcTranslator(irElasticSearchYml);
        sgAuthc = authcTranslator.getConfig();
        sg_frontend_authc = authcTranslator.getFrontEndConfig();
    }

    public MigrateConfig.SgAuthc getSg_frontend_authc() {
        return sg_frontend_authc;
    }
}
