package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationIR {
    // Password hashing
    String passwordHashingAlgorithm;

    // Anonymous access
    String anonymousUserName;
    String anonymousRoles;
    boolean anonymousAuthzException;

    // Token service
    boolean tokenEnabled;
    String tokenTimeout;

    // API Key
    boolean apiKeyEnabled; 
    String apiKeyCacheTtl; // Time-to-live for API Keys
    String maxKeys; // Maximum number of API keys
    String apiKeyInMemoryHashingAlgorithm; // From x-pack "xpack.security.authc.api_key.cache.hash_algo"
    String apiKeyRetentionPeriod; // Time after which a expired key can be deleted
    String apiKeyDeleteInterval; // Schedule for automatic deletion of expired keys
    String apiKeyDeleteTimeout;
    String apiKeyHashingAlgorithm; // From x-pack "xpack.security.authc.api_key.hashing.algorithm" (x-pack makes a distinction between in memory)

    // Domains
    Map<String, List<String>> domains = new HashMap<>();

    // realms collection
    Map<String, RealmIR> realms = new HashMap<>();

    public boolean getApiKeyEnabled() { return apiKeyEnabled; }
    public String getApiKeyCacheTtl() { return apiKeyCacheTtl; }
    public String getMaxTokens() { return maxKeys; }
    public Map<String, RealmIR> getRealms() { return realms; }

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

            realm.handleAttribute(attr, optionValue, keyPrefix + "realms." + type + "." + name + ".", configFile);
            return;
        }
        // Booleans
        else if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "token.enabled":
                    tokenEnabled = value;
                    break;

                case "anonymous.authz_exception":
                    anonymousAuthzException = value;
                    break;

                case "api_key.enabled":
                    apiKeyEnabled = value; 
                    break;

                default:
                    error = true;
            }
        // Strings
        } else if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, String.class)) {
            String value = (String) optionValue;
            switch (optionName) {
                case "password_hashing.algorithm":
                    passwordHashingAlgorithm = value;
                    break;

                case "anonymous.username":
                    anonymousUserName = value;
                    break;

                case "anonymous.roles":
                    anonymousRoles = value;
                    break;

                case "token.timeout":
                    tokenTimeout = value;
                    break;

                case "api_key.cache.ttl":
                    apiKeyCacheTtl = value;
                    break;

                case "api_key.cache.max_keys":
                    maxKeys = value;
                    break;

                case "api_key.cache.hash_algo":
                    apiKeyInMemoryHashingAlgorithm = value;
                    break;

                case "api_key.delete.retention_period":
                    apiKeyRetentionPeriod = value;
                    break;

                case "api_key.delete.interval":
                    apiKeyDeleteInterval = value;
                    break;

                case "api_key.delete.timeout":
                    apiKeyDeleteTimeout = value;
                    break;

                case "api_key.hashing.algorithm":
                    apiKeyHashingAlgorithm = value;
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
