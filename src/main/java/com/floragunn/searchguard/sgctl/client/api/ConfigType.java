package com.floragunn.searchguard.sgctl.client.api;

import java.io.File;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.ValidationError;

public enum ConfigType {
    CONFIG, INTERNALUSERS, ACTIONGROUPS, ROLES, ROLESMAPPING, TENANTS, BLOCKS, FRONTEND_CONFIG;

    public String getApiName() {
        return name().toLowerCase();
    }

    public String getFileName() {
        if (this == CONFIG) {
            return "sg_config.yml";
        } else if (this == INTERNALUSERS) {
            return "sg_internal_users.yml";
        } else if (this == ROLESMAPPING) {
            return "sg_roles_mapping.yml";
        } else {
            return "sg_" + name().toLowerCase() + ".yml";
        }
    }

    public static ConfigType get(String name) {
        return valueOf(name.toUpperCase());
    }

    public static ConfigType getFor(File file, DocNode content) throws ConfigValidationException {
        ValidationErrors validationErrors = new ValidationErrors();
        ValidatingDocNode vNode = new ValidatingDocNode(content, validationErrors);

        ConfigType result = vNode.get("_sg_meta.type").asEnum(ConfigType.class);

        if (result != null) {
            return result;
        }

        validationErrors.throwExceptionForPresentErrors();

        String name = file.getName();

        if (name.startsWith("sg_")) {
            name = name.substring(3);
        }

        for (ConfigType configType : values()) {
            if (name.toUpperCase().startsWith(configType.name())) {
                return configType;
            }

            if (name.replaceAll("_", "").toUpperCase().startsWith(configType.name())) {
                return configType;
            }
        }

        throw new ConfigValidationException(new ValidationError(null, "Unknown config type"));
    }
}
