package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationIR {
    // API Key
    boolean apiKeyEnabled; 
    String apiKeyCacheTtl; // Time-to-live for API Keys
    String maxTokens; // Maximum number of API keys

    // realms collection
    public Map<String, RealmIR> realms = new HashMap<>();

    public void handleOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        boolean error = false;

        // realms, they have this pattern: xpack.security.authc.realms.<type>.<name>.<setting>
        if (optionName.startsWith("realms.")) {
            String substring = optionName.substring("realms.".length());
            String[] parts = substring.split("\\.");

            if (parts.length < 3) {
                System.out.println("Invalid option: %s" + substring);
                return;
            }

            String type = parts[0]; // ldap, ...
            String name = parts[1]; // ldap1, ...
            String attr = String.join(".", Arrays.copyOfRange(parts, 2, parts.length));

            RealmIR realm = realms.computeIfAbsent(name, n -> RealmIR.create(type, n));

            realm.handleAttribute(attr, optionValue, keyPrefix, configFile);
            return;
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
