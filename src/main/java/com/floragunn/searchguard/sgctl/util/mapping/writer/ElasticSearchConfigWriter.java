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
    // Default values for TLS configuration
    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
    private static final String DEFAULT_KEYSTORE_KEYPASSWORD = "changeit";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private final IntermediateRepresentationElasticSearchYml ir;

    final static String FILE_NAME = "elasticsearch.yml";

    public ElasticSearchConfigWriter(IntermediateRepresentationElasticSearchYml ir) {
        this.ir = ir;
    }

    @Override
    public Object toBasicObject() {
        var contents = new LinkedHashMap<>(tlsMapWriter("transport", ir.getSslTls().getTransport()));
        contents.putAll(tlsMapWriter("http", ir.getSslTls().getHttp()));
        return contents;
    }

    private Map<String, Object> tlsMapWriter(String type, Tls tls) {
        String transportKeystoreType = Objects.toString(tls.getKeystoreType(), DEFAULT_KEYSTORE_TYPE);
        String transportKeystoreFilepath = Objects.toString(tls.getKeystorePath(), null);
        String transportTruststoreType = Objects.toString(tls.getTruststoreType(), DEFAULT_TRUSTSTORE_TYPE);
        String transportTruststoreFilepath = Objects.toString(tls.getTruststorePath(), null);

        String transportKeystorePassword = Objects.toString(tls.getKeystorePassword(), DEFAULT_KEYSTORE_PASSWORD);
        String transportKeystoreKeyPassword = Objects.toString(tls.getKeystoreKeyPassword(), DEFAULT_KEYSTORE_KEYPASSWORD);
        String transportTruststorePassword = Objects.toString(tls.getTruststorePassword(), DEFAULT_TRUSTSTORE_PASSWORD);
        final var prefix = "searchguard.ssl.";
        var contents = new LinkedHashMap<String, Object>();
        contents.put(prefix + type + ".enabled", String.valueOf(tls.getEnabled() || DEFAULT_ENABLED));
        contents.put(prefix + type + ".keystore_type", transportKeystoreType);
        contents.put(prefix + type + ".keystore_filepath", transportKeystoreFilepath);
        contents.put(prefix + type + ".keystore_password", transportKeystorePassword);
        contents.put(prefix + type + ".keystore_keypassword", transportKeystoreKeyPassword);
        contents.put(prefix + type + ".truststore_type", transportTruststoreType);
        contents.put(prefix + type + ".truststore_filepath", transportTruststoreFilepath);
        contents.put(prefix + type + ".truststore_password", transportTruststorePassword);
        contents.put(prefix + type + ".enabled_ciphers", tls.getCiphers());
        contents.put(prefix + type + ".enabled_protocols", tls.getSupportedProtocols());
        return contents;
    }
}