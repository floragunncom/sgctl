package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.Tls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private ElasticSearchConfigWriter() { }

    /**
     * Generates the Elasticsearch configuration file ({@code elasticsearch.yml}) from the
     * Intermediate Representation (IR) and writes it to the specified output directory.
     * It also prints a final migration report detailing actions and warnings.
     *
     * @param ir The intermediate representation containing the parsed configuration data.
     * @param outputDir The directory where the elasticsearch.yml file should be written.
     */
    public static void generateESConfigFromIR(IntermediateRepresentationElasticSearchYml ir, File outputDir) {
        System.out.println("--- DEBUG: START generateConfigsFromIR (Outputting defaults in elasticsearch.yml style) ---");

        MigrationReport report = new MigrationReport();

        try {
            // Pass the report object to the config creation method
            String configContent = createConfigStringFromIR(ir, report);

            writeStringConfig(configContent, outputDir, "elasticsearch.yml");

            System.out.println("--- DEBUG: SUCCESS! Default configuration written to: " + outputDir.getAbsolutePath() + File.separator + "elasticsearch.yml ---");

        } catch (IOException e) {
            System.err.println("--- DEBUG: ERROR! Configuration could not be written (IOException). ---");
            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("--- DEBUG: CRITICAL ERROR! Unexpected error during configuration generation. ---");
            e.printStackTrace();
        }

        System.out.println("--- DEBUG: END generateConfigsFromIR ---");
        //TODO: remove debug output
        report.printReport();
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
     * @param report The {@link MigrationReport} instance to log warnings and manual actions.
     * @return The complete configuration content as a String, formatted for {@code elasticsearch.yml}.
     * @throws IllegalStateException if the Transport or HTTP Tls configuration objects are missing ({@code null}).
     */
    private static String createConfigStringFromIR(IntermediateRepresentationElasticSearchYml ir, MigrationReport report) {
        final String FILE_NAME = "elasticsearch.yml";

        // Access the nested TLS configuration objects (assuming public fields/accessors)
        Tls transportTls = ir.sslTls.transport;
        Tls httpTls = ir.sslTls.http;

        if (transportTls == null || httpTls == null) {
            throw new IllegalStateException("Transport or HTTP Tls configuration is missing in the Intermediate Representation.");
        }

        // --- TRANSPORT TLS ---

        // Apply defaults or use IR values for non-password fields
        String transportKeystoreType = Objects.toString(transportTls.keystoreType, DEFAULT_KEYSTORE_TYPE);
        String transportKeystoreFilepath = Objects.toString(transportTls.keystorePath, null);
        String transportTruststoreType = Objects.toString(transportTls.truststoreType, DEFAULT_TRUSTSTORE_TYPE);
        String transportTruststoreFilepath = Objects.toString(transportTls.truststorePath, null);

        // Apply defaults or use IR values for password fields and log if default is used
        String transportKeystorePassword = Objects.toString(transportTls.keystorePassword, DEFAULT_KEYSTORE_PASSWORD);
        String transportKeystoreKeyPassword = Objects.toString(transportTls.keystoreKeyPassword, DEFAULT_KEYSTORE_KEYPASSWORD);
        String transportTruststorePassword = Objects.toString(transportTls.truststorePassword, DEFAULT_TRUSTSTORE_PASSWORD);

        // Check and report on 'changeit' defaults for passwords
        if (transportTls.keystorePassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.transport.keystore_password",
                    "Keystore password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }
        if (transportTls.keystoreKeyPassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.transport.keystore_keypassword",
                    "Keystore key password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }
        if (transportTls.truststorePassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.transport.truststore_password",
                    "Truststore password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }

        // Check for mandatory fields (transport truststore filepath)
        if (transportTruststoreFilepath == null) {
            report.addMissingParameter(FILE_NAME, "searchguard.ssl.transport.truststore_filepath", "searchguard.ssl.transport");
        }


        // --- HTTP TLS ---

        // 'enabled' output logic: use IR value, or fall back to DEFAULT_ENABLED (true).
        // WARNING: This logic overrides explicit 'false' if Tls.enabled is primitive 'boolean'.
        String httpEnabledOutput = String.valueOf(httpTls.enabled || DEFAULT_ENABLED);

        // Apply defaults or use IR values for non-password fields
        String httpKeystoreType = Objects.toString(httpTls.keystoreType, DEFAULT_KEYSTORE_TYPE);
        String httpKeystoreFilepath = Objects.toString(httpTls.keystorePath, null);
        String httpTruststoreType = Objects.toString(httpTls.truststoreType, DEFAULT_TRUSTSTORE_TYPE);
        String httpTruststoreFilepath = Objects.toString(httpTls.truststorePath, null);

        // Apply defaults or use IR values for password fields and log if default is used
        String httpKeystorePassword = Objects.toString(httpTls.keystorePassword, DEFAULT_KEYSTORE_PASSWORD);
        String httpKeystoreKeyPassword = Objects.toString(httpTls.keystoreKeyPassword, DEFAULT_KEYSTORE_KEYPASSWORD);
        String httpTruststorePassword = Objects.toString(httpTls.truststorePassword, DEFAULT_TRUSTSTORE_PASSWORD);

        // Check and report on 'changeit' defaults for passwords
        if (httpTls.keystorePassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.http.keystore_password",
                    "Keystore password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }
        if (httpTls.keystoreKeyPassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.http.keystore_keypassword",
                    "Keystore key password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }
        if (httpTls.truststorePassword == null) {
            report.addManualAction(FILE_NAME, "searchguard.ssl.http.truststore_password",
                    "Truststore password defaulted to 'changeit'. **MUST be updated** to actual password or secured via Elasticsearch Keystore.");
        }

        // Check for mandatory fields
        if (httpTruststoreFilepath == null) {
            report.addMissingParameter(FILE_NAME, "searchguard.ssl.http.truststore_filepath", "searchguard.ssl.http");
        }


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

                // HTTP TLS Configuration
                "searchguard.ssl.http.enabled: " + httpEnabledOutput + "\n" +
                "searchguard.ssl.http.keystore_type: " + httpKeystoreType + "\n" +
                "searchguard.ssl.http.keystore_filepath: " + httpKeystoreFilepath + "\n" +
                "searchguard.ssl.http.keystore_password: " + httpKeystorePassword + "\n" +
                "searchguard.ssl.http.keystore_keypassword: " + httpKeystoreKeyPassword + "\n" +
                "searchguard.ssl.http.truststore_type: " + httpTruststoreType + "\n" +
                "searchguard.ssl.http.truststore_filepath: " + httpTruststoreFilepath + "\n" +
                "searchguard.ssl.http.truststore_password: " + httpTruststorePassword + "\n";
    }

    /**
     * Writes the generated configuration content string to a file within the specified output directory.
     * It ensures the output directory exists before attempting to write the file.
     *
     * @param configContent The String content to be written to the file.
     * @param outputDir The directory where the file will be created.
     * @param filename The name of the file to create (e.g., "elasticsearch.yml").
     * @throws IOException if the directory cannot be created or the file cannot be written.
     * @throws IllegalArgumentException if the output directory is null.
     */
    private static void writeStringConfig(String configContent, File outputDir, String filename) throws IOException {
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        File outputFile = new File(outputDir, filename);

        Files.writeString(outputFile.toPath(), configContent);
    }
}