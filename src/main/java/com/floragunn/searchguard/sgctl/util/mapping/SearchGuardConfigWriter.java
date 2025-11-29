package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.Tls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


/**
 * Top-level configuration writer for Search Guard.
 * Generates config.yml and potentially other files from Intermediate Representation.
 */
public class SearchGuardConfigWriter {

    private SearchGuardConfigWriter() { }

    /**
     * Generiert alle notwendigen Search Guard Konfigurationsdateien aus der gefüllten IR.
     *
     * @param ir Die gefüllte Intermediate Representation.
     * @param outputDir Der Ordner, in den die config.yml geschrieben wird.
     */
    public static void generateConfigsFromIR(IntermediateRepresentationElasticSearchYml ir, File outputDir) {
        System.out.println("--- DEBUG: START generateConfigsFromIR ---");

        try {
            // Die IR ist bereits gefüllt durch den ElasticsearchYamlReader.
            SgConfigYml config = createConfigYml(ir);

            writeYamlConfig(config, outputDir, "sg_config.yml");

            System.out.println("--- DEBUG: SUCCESS! Configuration written to: " + outputDir.getAbsolutePath() + File.separator + "config.yml ---");

        } catch (IOException e) {
            System.err.println("--- DEBUG: ERROR! Configuration could not be written (IOException). ---");
            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("--- DEBUG: CRITICAL ERROR! Unexpected error during configuration generation. ---");
            e.printStackTrace();
        }

        System.out.println("--- DEBUG: END generateConfigsFromIR ---");
    }

    /**
     * Creates the SgConfigYml object and populates it with mapped TLS/SSL settings from the IR.
     */
    private static SgConfigYml createConfigYml(IntermediateRepresentationElasticSearchYml ir) {
        SgConfigYml config = new SgConfigYml();

        System.out.println("DEBUG: Starting createConfigYml...");

        Tls transportTls = ir.sslTls != null && ir.sslTls.transport != null ? ir.sslTls.transport : new Tls();
        Tls httpTls = ir.sslTls != null && ir.sslTls.http != null ? ir.sslTls.http : new Tls();


        Map<String, Object> sgMetaMap = new LinkedHashMap<>();
        sgMetaMap.put("type", "config");
        sgMetaMap.put("config_version", 2);

        config.setSgMeta(sgMetaMap);

        Map<String, Object> sslConfigMap = config.getSslConfigMap();

        sslConfigMap.put("transport", mapTlsToConfigYml(transportTls));
        sslConfigMap.put("http", mapTlsToConfigYml(httpTls));

        // (Optional: Hier würde die http_settings-Logik hinkommen)

        System.out.println("DEBUG: createConfigYml completed.");

        return config;
    }

    /**
     * Mappt TLS-Einstellungen von Tls-Objekt auf Map für config.yml.
     * Fügt Platzhalter für null-Felder ein und protokolliert nur Nicht-Null-Felder.
     */
    private static Map<String, Object> mapTlsToConfigYml(Tls tls) {
        if (tls == null) {
            System.out.println("DEBUG: Tls object is null. Using default: enabled=false.");
            Map<String, Object> defaultMap = new LinkedHashMap<>();
            defaultMap.put("enabled", Boolean.FALSE);
            return defaultMap;
        }

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("enabled", tls.enabled);
        System.out.println("DEBUG: Mapped (NOT NULL): enabled = " + tls.enabled);


        if (tls.keystorePath != null) {
            map.put("keystore_filepath", tls.keystorePath);
            System.out.println("DEBUG: Mapped (NOT NULL): keystore_filepath = " + tls.keystorePath);
        } else {
            map.put("keystore_filepath", createPlaceholder("keystore_filepath"));
        }

        if (tls.keystorePassword != null) {
            map.put("keystore_password", tls.keystorePassword);
            System.out.println("DEBUG: Mapped (NOT NULL): keystore_password = " + tls.keystorePassword);
        } else {
            map.put("keystore_password", createPlaceholder("keystore_password"));
        }

        if (tls.keystoreKeyPassword != null) {
            map.put("keystore_key_password", tls.keystoreKeyPassword);
            System.out.println("DEBUG: Mapped (NOT NULL): keystore_key_password = " + tls.keystoreKeyPassword);
        } else {
            map.put("keystore_key_password", createPlaceholder("keystore_key_password"));
        }

        if (tls.keystoreType != null) {
            map.put("keystore_type", tls.keystoreType);
            System.out.println("DEBUG: Mapped (NOT NULL): keystore_type = " + tls.keystoreType);
        } else {
            map.put("keystore_type", createPlaceholder("keystore_type"));
        }


        if (tls.truststorePath != null) {
            map.put("truststore_filepath", tls.truststorePath);
            System.out.println("DEBUG: Mapped (NOT NULL): truststore_filepath = " + tls.truststorePath);
        } else {
            map.put("truststore_filepath", createPlaceholder("truststore_filepath"));
        }

        if (tls.truststorePassword != null) {
            map.put("truststore_password", tls.truststorePassword);
            System.out.println("DEBUG: Mapped (NOT NULL): truststore_password = " + tls.truststorePassword);
        } else {
            map.put("truststore_password", createPlaceholder("truststore_password"));
        }

        if (tls.truststoreType != null) {
            map.put("truststore_type", tls.truststoreType);
            System.out.println("DEBUG: Mapped (NOT NULL): truststore_type = " + tls.truststoreType);
        } else {
            map.put("truststore_type", createPlaceholder("truststore_type"));
        }


        if (tls.certificatePath != null) {
            map.put("certificate_filepath", tls.certificatePath);
            System.out.println("DEBUG: Mapped (NOT NULL): certificate_filepath = " + tls.certificatePath);
        } else {
            map.put("certificate_filepath", createPlaceholder("certificate_filepath"));
        }

        if (tls.privateKeyPath != null) {
            map.put("private_key_filepath", tls.privateKeyPath);
            System.out.println("DEBUG: Mapped (NOT NULL): private_key_filepath = " + tls.privateKeyPath);
        } else {
            map.put("private_key_filepath", createPlaceholder("private_key_filepath"));
        }

        if (tls.privateKeyPassword != null) {
            map.put("private_key_password", tls.privateKeyPassword);
            System.out.println("DEBUG: Mapped (NOT NULL): private_key_password = " + tls.privateKeyPassword);
        } else {
            map.put("private_key_password", createPlaceholder("private_key_password"));
        }


        if (tls.verificationMode != null) {
            map.put("verification_mode", tls.verificationMode);
            System.out.println("DEBUG: Mapped (NOT NULL): verification_mode = " + tls.verificationMode);
        } else {
            map.put("verification_mode", createPlaceholder("verification_mode"));
        }

        if (tls.clientAuthMode != null) {
            map.put("client_authentication", tls.clientAuthMode);
            System.out.println("DEBUG: Mapped (NOT NULL): client_authentication = " + tls.clientAuthMode);
        } else {
            map.put("client_authentication", createPlaceholder("client_authentication"));
        }

        if (tls.certificateAuthorities != null && !tls.certificateAuthorities.isEmpty()) {
            map.put("certificate_authorities", tls.certificateAuthorities);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): certificate_authorities = " + tls.certificateAuthorities.size() + " entries");
        }
        if (tls.supportedProtocols != null && !tls.supportedProtocols.isEmpty()) {
            map.put("supported_protocols", tls.supportedProtocols);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): supported_protocols = " + tls.supportedProtocols.size() + " entries");
        }
        if (tls.ciphers != null && !tls.ciphers.isEmpty()) {
            map.put("ciphers", tls.ciphers);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): ciphers = " + tls.ciphers.size() + " entries");
        }

        if (tls.allowedIPs != null && !tls.allowedIPs.isEmpty()) {
            map.put("filter_allow", tls.allowedIPs);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): filter_allow = " + tls.allowedIPs.size() + " entries");
        }
        if (tls.deniedIPs != null && !tls.deniedIPs.isEmpty()) {
            map.put("filter_deny", tls.deniedIPs);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): filter_deny = " + tls.deniedIPs.size() + " entries");
        }
        if (tls.remoteClusterAllowedIPs != null && !tls.remoteClusterAllowedIPs.isEmpty()) {
            map.put("remote_cluster_filter_allow", tls.remoteClusterAllowedIPs);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): remote_cluster_filter_allow = " + tls.remoteClusterAllowedIPs.size() + " entries");
        }
        if (tls.remoteClusterDeniedIPs != null && !tls.remoteClusterDeniedIPs.isEmpty()) {
            map.put("remote_cluster_filter_deny", tls.remoteClusterDeniedIPs);
            System.out.println("DEBUG: Mapped (NOT NULL LIST): remote_cluster_filter_deny = " + tls.remoteClusterDeniedIPs.size() + " entries");
        }

        return map;
    }

    private static void writeYamlConfig(Object configObject, File outputDir, String filename) throws IOException {
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        File outputFile = new File(outputDir, filename);

        String yamlContent = DocWriter.yaml().writeAsString(configObject);

        Files.writeString(outputFile.toPath(), yamlContent);
    }


    /**
     * Top-level class for the entire config.yml file. Implements Document for DocWriter.
     */
    public static class SgConfigYml implements Document {

        private final Map<String, Object> rootConfig = new LinkedHashMap<>();
        public final Map<String, Object> dynamicSettings = new LinkedHashMap<>();

        public SgConfigYml() {
            Map<String, Object> ssl = new LinkedHashMap<>();
            this.dynamicSettings.put("ssl", ssl);

            Map<String, Object> sgConfigBlock = new LinkedHashMap<>();
            sgConfigBlock.put("dynamic", this.dynamicSettings);

            rootConfig.put("_sg_meta", null);
            rootConfig.put("sg_config", sgConfigBlock);
        }

        public Map<String, Object> getSslConfigMap() {
            return (Map<String, Object>) this.dynamicSettings.get("ssl");
        }

        @Override
        public Object toBasicObject() {
            return this.rootConfig;
        }

        public void setSgMeta(Object sgMeta) {
            rootConfig.put("_sg_meta", sgMeta);
        }
    }

    private static String createPlaceholder(String fieldName) {
        return "[FEHLT: " + fieldName + "]";
    }
}