package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationIR {
    // API Key
    boolean apiKeyEnabled; 
    String apiKeyCacheTtl; // Time-to-live for API Keys
    String maxTokens; // Maximum number of API keys

    // realms collection
    Map<String, RealmIR> realms = new HashMap<>();

    public void handleOptions(String optionName, Object optionValue) {
        boolean error = false;

        // realms, they have this pattern: xpack.security.authc.realms.<type>.<name>.<setting>
        if (optionName.startsWith("realms.")) {

        }

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "api_key.enabled":
                    apiKeyEnabled = value; 
                    break;

                default:
                    error = true;
            }
        } else if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, String.class)) {
            String value = (String) optionValue;
            switch (optionName) {
                case "api_key.cache.ttl":
                    apiKeyCacheTtl = value;
                    break;

                case "api_key.cache.max_keys":
                    maxTokens = value;
                    break;

                default:
                    error = true;
            }
        }

        if (error) {
            System.out.println("Invalid option of type " + optionValue.getClass() + ": " + optionName + " = " + optionValue);
        }
    }
}
