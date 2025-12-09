package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.Tls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * Utility class responsible for generating the final Elasticsearch configuration file ({@code elasticsearch.yml})
 * based on an Intermediate Representation (IR) of the configuration data.
 * <p>
 * This class handles applying default values for TLS settings if they are not explicitly
 * set (i.e., are null) in the {@link Tls} objects within the IR. It also logs important
 * warnings and manual actions, such as the use of default passwords, to a {@link MigrationReport}.
 */
public class ElasticSearchConfigWriter {
    // Default values for TLS configuration
    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
    private static final String DEFAULT_KEYSTORE_KEYPASSWORD = "changeit";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    public static void writeElasticSearchYml(IntermediateRepresentationElasticSearchYml ir, File outputDir) throws IOException {
        String configContent = createConfigStringFromIR(ir);

        writeElasticSearchConfig(configContent, outputDir);
    }

    /**
     * Creates the elasticsearch.yml configuration content string by applying values
     * from the Intermediate Representation (IR) and logging required manual actions to the report.
     * <p>
     * It uses the configured value from the Tls object if it is not null;
     * otherwise, it falls back to the hardcoded default value and logs a manual action
     * for default passwords.
     *
     * @param ir The intermediate representation containing the TLS configuration data.
     * @return The complete configuration content as a String, formatted for {@code elasticsearch.yml}.
     * @throws IllegalStateException if the Transport or HTTP Tls configuration objects are missing ({@code null}).
     */
    public static String createConfigStringFromIR(IntermediateRepresentationElasticSearchYml ir) {
        final String FILE_NAME = "elasticsearch.yml";

        // Access the nested TLS configuration objects (assuming public fields/accessors)
        Tls transportTls = ir.getSslTls().getTransport();
        Tls httpTls = ir.getSslTls().getHttp();

        if (transportTls == null || httpTls == null) {
            throw new IllegalStateException("Transport or HTTP Tls configuration is missing in the Intermediate Representation.");
        }

        // --- TRANSPORT TLS ---

        // Apply defaults or use IR values for non-password fields
        String transportKeystoreType = Objects.toString(transportTls.getKeystoreType(), DEFAULT_KEYSTORE_TYPE);
        String transportKeystoreFilepath = Objects.toString(transportTls.getKeystorePath(), null);
        String transportTruststoreType = Objects.toString(transportTls.getTruststoreType(), DEFAULT_TRUSTSTORE_TYPE);
        String transportTruststoreFilepath = Objects.toString(transportTls.getTruststorePath(), null);

        // Apply defaults or use IR values for password fields and log if default is used
        String transportKeystorePassword = Objects.toString(transportTls.getKeystorePassword(), DEFAULT_KEYSTORE_PASSWORD);
        String transportKeystoreKeyPassword = Objects.toString(transportTls.getKeystoreKeyPassword(), DEFAULT_KEYSTORE_KEYPASSWORD);
        String transportTruststorePassword = Objects.toString(transportTls.getTruststorePassword(), DEFAULT_TRUSTSTORE_PASSWORD);

        // --- HTTP TLS ---
        // 'enabled' output logic: use IR value, or fall back to DEFAULT_ENABLED (true).
        // WARNING: This logic overrides explicit 'false' if Tls.enabled is primitive 'boolean'.
        String httpEnabledOutput = String.valueOf(httpTls.getEnabled() || DEFAULT_ENABLED);

        // Apply defaults or use IR values for non-password fields
        String httpKeystoreType = Objects.toString(httpTls.getKeystoreType(), DEFAULT_KEYSTORE_TYPE);
        String httpKeystoreFilepath = Objects.toString(httpTls.getKeystorePath(), null);
        String httpTruststoreType = Objects.toString(httpTls.getTruststoreType(), DEFAULT_TRUSTSTORE_TYPE);
        String httpTruststoreFilepath = Objects.toString(httpTls.getTruststorePath(), null);

        // Apply defaults or use IR values for password fields and log if default is used
        String httpKeystorePassword = Objects.toString(httpTls.getKeystorePassword(), DEFAULT_KEYSTORE_PASSWORD);
        String httpKeystoreKeyPassword = Objects.toString(httpTls.getKeystoreKeyPassword(), DEFAULT_KEYSTORE_KEYPASSWORD);
        String httpTruststorePassword = Objects.toString(httpTls.getTruststorePassword(), DEFAULT_TRUSTSTORE_PASSWORD);

        MigrationReport.shared.addManualAction("elasticsearch.yml", "searchguard.ssl.client.external_context_id", "If you set wrong values here this this could be a security risk");
        MigrationReport.shared.addManualAction("elasticsearch.yml", "searchguard.ssl.transport.principal_extractor_class", "If you set wrong values here this this could be a security risk");

        // Assemble the final configuration string
        return "" +
                // Transport TLS Configuration
                "searchguard.ssl.transport.keystore_type: " + transportKeystoreType + "\n" +
                "searchguard.ssl.transport.keystore_filepath: " + transportKeystoreFilepath + "\n" +
                "searchguard.ssl.transport.keystore_password: " + transportKeystorePassword + "\n" +
                "searchguard.ssl.transport.keystore_keypassword: " + transportKeystoreKeyPassword + "\n" +
                "searchguard.ssl.transport.truststore_type: " + transportTruststoreType + "\n" +
                "searchguard.ssl.transport.truststore_filepath: " + transportTruststoreFilepath + "\n" +
                "searchguard.ssl.transport.truststore_password: " + transportTruststorePassword + "\n" +

                toYamlList("searchguard.ssl.transport.enabled_ciphers", transportTls.getCiphers()) +
                toYamlList("searchguard.ssl.transport.enabled_protocols", transportTls.getSupportedProtocols()) +

                // HTTP TLS Configuration
                "searchguard.ssl.http.enabled: " + httpEnabledOutput + "\n" +
                "searchguard.ssl.http.keystore_type: " + httpKeystoreType + "\n" +
                "searchguard.ssl.http.keystore_filepath: " + httpKeystoreFilepath + "\n" +
                "searchguard.ssl.http.keystore_password: " + httpKeystorePassword + "\n" +
                "searchguard.ssl.http.keystore_keypassword: " + httpKeystoreKeyPassword + "\n" +
                "searchguard.ssl.http.truststore_type: " + httpTruststoreType + "\n" +
                "searchguard.ssl.http.truststore_filepath: " + httpTruststoreFilepath + "\n" +
                "searchguard.ssl.http.truststore_password: " + httpTruststorePassword + "\n" +

                toYamlList("searchguard.ssl.http.enabled_ciphers", transportTls.getCiphers()) +
                toYamlList("searchguard.ssl.http.enabled_protocols", transportTls.getSupportedProtocols());

    }
    public static String toYamlList(String key, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(":\n");
        for (String v : values) {
            sb.append("  - \"").append(v).append("\"\n");
        }
        return sb.toString();
    }
    /**
     * Writes the generated configuration content string to a file within the specified output directory.
     * It ensures the output directory exists before attempting to write the file.
     *
     * @param configContent The String content to be written to the file.
     * @param outputDir The directory where the file will be created.
     * @throws IOException if the directory cannot be created or the file cannot be written.
     * @throws IllegalArgumentException if the output directory is null.
     */
    public static void writeElasticSearchConfig(String configContent, File outputDir) throws IOException {
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        File outputFile = new File(outputDir, "elasticsearch.yml");

        Files.writeString(outputFile.toPath(), configContent);
    }
}