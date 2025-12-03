package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;

/**
 * Top-level configuration writer for Search Guard.
 * Generates sg_authc.yml with LDAP, SAML, OIDC realms mapped.
 */
public class SearchGuardConfigWriter {

    private SGAuthcTranslator.Configs authcConfigs;

    public SearchGuardConfigWriter(IntermediateRepresentationElasticSearchYml ir) {
        authcConfigs = SGAuthcTranslator.createAuthcConfig(ir);
    }



}
