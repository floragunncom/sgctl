package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

/**
 * Intermediate representation for authentication-related options read from {@code elasticsearch.yml}.
 */
public class AuthenticationIR {
    private static final String THIS_FILE = "elasticsearch.yml";
    private static final String REALMS_PREFIX = "realms.";
    private static final Logger LOG = Logger.getLogger(AuthenticationIR.class.getName());

    // Password hashing
    private String passwordHashingAlgorithm;

    // Anonymous access
    private String anonymousUserName;
    private String anonymousRoles;
    private boolean anonymousAuthzException;

    // Token service
    private boolean tokenEnabled;
    private String tokenTimeout;

    // API Key
    private boolean apiKeyEnabled;
    private String apiKeyCacheTtl; // Time-to-live for API Keys
    private String maxKeys; // Maximum number of API keys
    private String apiKeyInMemoryHashingAlgorithm; // From x-pack "xpack.security.authc.api_key.cache.hash_algo"
    private String apiKeyRetentionPeriod; // Time after which a expired key can be deleted
    private String apiKeyDeleteInterval; // Schedule for automatic deletion of expired keys
    private String apiKeyDeleteTimeout;
    private String apiKeyHashingAlgorithm; // From x-pack "xpack.security.authc.api_key.hashing.algorithm" (x-pack makes a distinction between in memory)

    // realms collection
    private final Map<String, RealmIR> realms = new HashMap<>();
    private final Map<String, RealmIR> realmsView = Collections.unmodifiableMap(realms);

    // Getter
    /** Returns the configured password hashing algorithm. */
    public String getPasswordHashingAlgoritm() { return passwordHashingAlgorithm; }
    /** Returns the anonymous user name or {@code null}. */
    public String getAnonymousUserName() { return anonymousUserName; }
    /** Returns comma-separated anonymous roles. */
    public String getAnonymousRoles() { return  anonymousRoles; }
    /** Returns whether anonymous authz exceptions are enabled. */
    public boolean getAnonymousAuthzException() { return anonymousAuthzException; }
    /** Returns whether tokens are enabled. */
    public boolean getTokenEnabled() { return tokenEnabled; }
    /** Returns the token timeout value. */
    public String getTokenTimeout() { return tokenTimeout; }
    /** Returns true if API key support is enabled. */
    public boolean getApiKeyEnabled() { return apiKeyEnabled; }
    /** Returns the API key cache TTL. */
    public String getApiKeyCacheTtl() { return apiKeyCacheTtl; }
    /** Returns the maximum number of API keys. */
    public String getMaxTokens() { return maxKeys; }
    /** Returns the in-memory hashing algorithm for API keys. */
    public String getApiKeyInMemoryHashingAlgorithm() { return apiKeyInMemoryHashingAlgorithm; }
    /** Returns the retention period for expired API keys. */
    public String getApiKeyRetentionPeriod() { return apiKeyRetentionPeriod; }
    /** Returns the deletion interval for API keys. */
    public String getApiKeyDeleteInterval() { return apiKeyDeleteInterval; }
    /** Returns the deletion timeout for API keys. */
    public String getApiKeyDeleteTimeout() { return apiKeyDeleteTimeout; }
    /** Returns the hashing algorithm for API keys. */
    public String getApiKeyHashingAlgorithm() { return apiKeyHashingAlgorithm; }
    /** Returns an immutable view of configured realms. */
    public Map<String, RealmIR> getRealms() { return realmsView; }

    /**
     * Handles a single authentication-related option and records migration results.
     *
     * @param optionName option name relative to the xpack authc prefix
     * @param optionValue option value parsed from the config
     * @param keyPrefix prefix used for reporting
     * @param configFile source configuration file
     */
    public void handleOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        boolean keyKnown = true;

        // realms, they have this pattern: xpack.security.authc.realms.<type>.<name>.<setting>
        if (optionName.startsWith(REALMS_PREFIX)) {
            String substring = optionName.substring(REALMS_PREFIX.length());
            String[] parts = substring.split("\\.");

            if (parts.length < 3) {
                LOG.warning(() -> "Invalid option: " + substring);
                return;
            }

            String type = parts[0]; // ldap, ...
            String name = parts[1]; // ldap1, ...
            String attr = String.join(".", Arrays.copyOfRange(parts, 2, parts.length));

            RealmIR realm = realms.computeIfAbsent(name, n -> RealmIR.create(type, n));

            realm.handleAttribute(attr, optionValue, keyPrefix + REALMS_PREFIX + type + "." + name + ".", configFile);
            return;
        }
        // Booleans
        else if (IntermediateRepresentationElasticSearchYml.isType(optionValue, Boolean.class)) {
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
                    keyKnown = false;
            }
        // Strings
        } else if (IntermediateRepresentationElasticSearchYml.isType(optionValue, String.class)) {
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
                    keyKnown = false;
            }
        }

        if (!keyKnown) {
            MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + optionName, keyPrefix + optionName);
        }
    }
}
