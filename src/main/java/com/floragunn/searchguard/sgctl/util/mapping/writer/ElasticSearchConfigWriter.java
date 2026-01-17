/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.Tls;

import java.util.*;

/**
 * Utility class responsible for generating the final Elasticsearch configuration file ({@code elasticsearch.yml})
 * based on an Intermediate Representation (IR) of the configuration data.
 * <p>
 * This class handles applying default values for TLS settings if they are not explicitly
 * set (i.e., are null) in the {@link Tls} objects within the IR. It also logs important
 * warnings and manual actions, such as the use of default passwords, to a {@link MigrationReport}.
 */
public class ElasticSearchConfigWriter implements Document<ElasticSearchConfigWriter> {
    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
    private static final String DEFAULT_KEYSTORE_KEYPASSWORD = "changeit";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private static final String PLACEHOLDER = "changeit";
    private static final List<Object> EMPTY_LIST= new ArrayList<>();
    private final IntermediateRepresentationElasticSearchYml ir;
    private final Map<String, Object> tlsTransportMap;
    private final Map<String, Object> tlsHTTPMap;
    private final Map<String, Object> defaultsMap;
    final static String FILE_NAME = "generated elasticsearch.yml";
    final static MigrationReport report = MigrationReport.shared;

    private static final Map<String, String> OLD_PARAMETER_NAMES = Map.ofEntries(
            Map.entry("searchguard.ssl.transport.enabled", "xpack.security.transport.ssl.enabled"),
            Map.entry("searchguard.ssl.transport.keystore_type", "xpack.security.transport.ssl.keystore.type"),
            Map.entry("searchguard.ssl.transport.keystore_filepath", "xpack.security.transport.ssl.keystore.path"),
            Map.entry("searchguard.ssl.transport.keystore_password", "xpack.security.transport.ssl.keystore.password"),
            Map.entry("searchguard.ssl.transport.keystore_keypassword", "xpack.security.transport.ssl.keystore.key_password"),
            Map.entry("searchguard.ssl.transport.truststore_type", "xpack.security.transport.ssl.truststore.type"),
            Map.entry("searchguard.ssl.transport.truststore_filepath", "xpack.security.transport.ssl.truststore.path"),
            Map.entry("searchguard.ssl.transport.truststore_password", "xpack.security.transport.ssl.truststore.password"),

            Map.entry("searchguard.ssl.http.enabled", "xpack.security.http.ssl.enabled"),
            Map.entry("searchguard.ssl.http.keystore_type", "xpack.security.http.ssl.keystore.type"),
            Map.entry("searchguard.ssl.http.keystore_filepath", "xpack.security.http.ssl.keystore.path"),
            Map.entry("searchguard.ssl.http.keystore_password", "xpack.security.http.ssl.keystore.password"),
            Map.entry("searchguard.ssl.http.keystore_keypassword", "xpack.security.http.ssl.keystore.key_password"),
            Map.entry("searchguard.ssl.http.truststore_type", "xpack.security.http.ssl.truststore.type"),
            Map.entry("searchguard.ssl.http.truststore_filepath", "xpack.security.http.ssl.truststore.path"),
            Map.entry("searchguard.ssl.http.truststore_password", "xpack.security.http.ssl.truststore.password"),

            Map.entry("searchguard.ssl.transport.enabled_ciphers", "xpack.security.transport.ssl.enabled_ciphers"),
            Map.entry("searchguard.ssl.transport.enabled_protocols", "xpack.security.transport.ssl.enabled_protocols"),

            Map.entry("searchguard.ssl.http.enabled_ciphers", "xpack.security.http.ssl.enabled_ciphers"),
            Map.entry("searchguard.ssl.http.enabled_protocols", "xpack.security.http.ssl.enabled_protocols")
    );

    public ElasticSearchConfigWriter(IntermediateRepresentationElasticSearchYml ir) {
        this.ir = ir;
        tlsTransportMap = tlsMapWriter("transport", ir.getSslTls().getTransport());
        tlsHTTPMap = tlsMapWriter("http", ir.getSslTls().getHttp());
        defaultsMap = defaultSearchGuardConfig();
        report.addInfo(FILE_NAME, "Used default for generated elasticsearch.yml");
    }

    @Override
    public Object toBasicObject() {
        var contents = new LinkedHashMap<>(ir.getParsedElasticsearchYAML());
        contents.putAll(tlsTransportMap);
        contents.putAll(tlsHTTPMap);
        contents.putAll(defaultsMap);
        contents.putAll(defaultSearchGuardConfig());
        contents.put("xpack.security.enabled", false);
        return contents;
    }

    private Map<String, Object> tlsMapWriter(String type, Tls tls) {
        final String prefix = "searchguard.ssl.";
        var contents = new LinkedHashMap<String, Object>();
        contents.put(prefix + type + ".enabled", logDefaultIfUsed(prefix + type + ".enabled", tls.getEnabled(), DEFAULT_ENABLED));
        contents.put(prefix + type + ".keystore_type", logDefaultIfUsed(prefix + type + ".keystore_type", tls.getKeystoreType(), DEFAULT_KEYSTORE_TYPE));
        contents.put(prefix + type + ".keystore_filepath", logDefaultIfUsed(prefix + type + ".keystore_filepath", tls.getKeystorePath(), null));
        contents.put(prefix + type + ".keystore_password", logDefaultIfUsed(prefix + type + ".keystore_password", tls.getKeystorePassword(), DEFAULT_KEYSTORE_PASSWORD));
        contents.put(prefix + type + ".keystore_keypassword", logDefaultIfUsed(prefix + type + ".keystore_keypassword", tls.getKeystoreKeyPassword(), DEFAULT_KEYSTORE_KEYPASSWORD));
        contents.put(prefix + type + ".truststore_type", logDefaultIfUsed(prefix + type + ".truststore_type", tls.getTruststoreType(), DEFAULT_TRUSTSTORE_TYPE));
        contents.put(prefix + type + ".truststore_filepath", logDefaultIfUsed(prefix + type + ".truststore_filepath", tls.getTruststorePath(), null));
        contents.put(prefix + type + ".truststore_password", logDefaultIfUsed(prefix + type + ".truststore_password", tls.getTruststorePassword(), DEFAULT_TRUSTSTORE_PASSWORD));

        contents.put(prefix + type + ".enabled_ciphers", logDefaultIfUsed(prefix + type + ".enabled_ciphers", tls.getCiphers(), null));
        contents.put(prefix + type + ".enabled_protocols", logDefaultIfUsed(prefix + type + ".enabled_protocols", tls.getSupportedProtocols(), null));

        return contents;
    }

    private <T> T logDefaultIfUsed(String fullFieldName, T value, T defaultValue) {
        if (value == null) {
            MigrationReport.shared.addWarning(FILE_NAME, fullFieldName, " is not found. Default is used: " + defaultValue);
            return defaultValue;
        } else {
            MigrationReport.shared.addMigrated(FILE_NAME, OLD_PARAMETER_NAMES.get(fullFieldName), fullFieldName);
            return value;
        }
    }

    //TODO: Wait for change of Migrationreport for changeit, not explicit defaults are ignored
    private static Map<String,Object> defaultSearchGuardConfig() {
        var contents = new LinkedHashMap<String,Object>();
        contents.put("searchguard.ssl.http.crl.validate", false);
        contents.put("searchguard.ssl.http.crl.file_path", null);
        contents.put("searchguard.ssl.http.crl.prefer_crlfile_over_ocsp", false);
        contents.put("searchguard.ssl.http.crl.check_only_end_entities", true);
        contents.put("searchguard.ssl.http.crl.disable_ocsp", false);
        contents.put("searchguard.ssl.http.crl.disable_crldp", false);
        contents.put("searchguard.ssl.http.crl.validation_date", -1);

        contents.put("searchguard.enterprise_modules_enabled", true);

        contents.put("searchguard.nodes_dn", PLACEHOLDER);
        contents.put("searchguard.authcz.admin_dn", PLACEHOLDER);

        contents.put("searchguard.restapi.roles_enabled", EMPTY_LIST);

        contents.put("searchguard.audit.enable_rest", true);
        contents.put("searchguard.audit.enable_transport", false);
        contents.put("searchguard.audit.resolve_bulk_requests", false);

        contents.put("searchguard.audit.threadpool.size", 10);
        contents.put("searchguard.audit.threadpool.max_queue_len", 100000);

        contents.put("searchguard.audit.enable_request_details", false);
        contents.put("searchguard.audit.ignore_users", PLACEHOLDER);

        contents.put("searchguard.audit.config.index", "auditlog6");
        return contents;
    }
}