package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;


public class SAMLTranslator extends RealmTranslator {
    public SAMLTranslator() {
        isFrontEnd = true;
    }
    private String convertAttributesToUserMapping(String attributeName) {
        if (attributeName == null || attributeName.isEmpty()) {
            return null;
        }

        // Handle special NameID formats
        if (attributeName.equalsIgnoreCase("nameid") ||
                attributeName.equalsIgnoreCase("nameid:persistent") ||
                attributeName.equalsIgnoreCase("nameid:transient")) {
            return "saml_response.name_id";
        }

        // Handle common SAML attributes
        // X-Pack attribute name -> SearchGuard FLX JSON path
        String lowerAttribute = attributeName.toLowerCase();

        return switch (lowerAttribute) {
            case "email", "mail" -> "saml_response.attributes.email";
            case "uid", "userid" -> "saml_response.attributes.uid";
            case "name", "displayname" -> "saml_response.attributes.name";
            case "groups", "roles" -> "saml_response.attributes.groups";
            default -> "saml_response.attributes." + lowerAttribute + "";
        };
    }

    @Override
    public MigrateConfig.NewAuthDomain translate(RealmIR originalIR) {
        RealmIR.SamlRealmIR ir = (RealmIR.SamlRealmIR) originalIR;

        addOptionalConfigProperty("saml.idp.metadata_url", ir.getIdpMetadataPath());
        //TODO: Change getIdpEntityID to current version
        //addOptionalConfigProperty("saml.idp.entity_id", ir.getIdpEntityID());

        addOptionalConfigProperty("saml.sp.entity_id", ir.getSpEntityID());

        // Username mapping from attributes.principal
        String userNameMapping = convertAttributesToUserMapping(ir.getAttributesPrincipal());
        addOptionalConfigProperty("user_mapping.user_name.from", userNameMapping);

        // SP ACS - convert X-Pack format to FLX format
        String spAcs = ir.getSpAcs();
        if (spAcs != null && !spAcs.isEmpty()) {
            String convertedAcs = spAcs.contains("/api/security/saml/callback")
                    ? spAcs.replace("/api/security/saml/callback", "/searchguard/saml/acs")
                    : spAcs;
            addOptionalConfigProperty("saml.sp.acs", convertedAcs);
        }
        /*
        // Roles mapping - extract from X-Pack if available
        if (ir.getAttributesGroups() != null) {
            String rolesMapping = convertAttributesToRolesMapping(ir.getAttributesGroups());
            addOptionalConfigProperty("user_mapping.roles.from", rolesMapping);
        } else {
            MigrationReport.shared.addManualAction(SG_FRONTEND_AUTHC_FILE_NAME, "SAML Roles", "Configure user_mapping.roles.from or use sg_roles_mapping.yml");
        }

         */

        return new MigrateConfig.NewAuthDomain(
                ir.getType(),
                null,
                null,
                null,
                config,
                null
        );
    }
}
