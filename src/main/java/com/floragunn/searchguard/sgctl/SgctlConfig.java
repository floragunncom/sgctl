package com.floragunn.searchguard.sgctl;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.floragunn.codova.config.net.TLSConfig;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ConfigVariableProviders;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.JsonValidationError;
import com.floragunn.codova.validation.errors.ValidationError;

public class SgctlConfig implements Document {

    private Map<String, Cluster> clusters = new LinkedHashMap<>();

    public static SgctlConfig read(File configFile) throws SgctlException {
        try {
            if (!configFile.exists()) {
                return new SgctlConfig();
            }

            Map<String, Object> config;

            try {
                config = DocReader.yaml().readObject(configFile);
            } catch (JsonProcessingException e) {
                throw new ConfigValidationException(new JsonValidationError(null, e));
            } catch (IOException e) {
                throw new SgctlException("Error while reading " + configFile + ": " + e, e);
            }

            SgctlConfig result = new SgctlConfig();
            ValidationErrors validationErrors = new ValidationErrors();

            for (Map.Entry<String, Object> entry : config.entrySet()) {
                if (!(entry.getValue() instanceof Map)) {
                    validationErrors.add(new ValidationError(entry.getKey(), "Must be an object"));
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> clusterConfig = (Map<String, Object>) entry.getValue();

                try {
                    result.clusters.put(entry.getKey(), Cluster.parse(clusterConfig));
                } catch (ConfigValidationException e) {
                    validationErrors.add(entry.getKey(), e);
                }
            }

            validationErrors.throwExceptionForPresentErrors();

            return result;
        } catch (ConfigValidationException e) {
            throw new SgctlException("File sgctl.yml is invalid:\n" + e.getValidationErrors(), e).debugDetail(e.toDebugString());
        }
    }

    public void write(File configDir) throws SgctlException {
        if (!configDir.exists()) {
            if (!configDir.mkdir()) {
                throw new SgctlException("Could not create directory " + configDir + ")");
            }
        }

        File configFile = new File(configDir, "sgctl.yml");

        try {
            DocWriter.yaml().write(configFile, this.toMap());
        } catch (IOException e) {
            throw new SgctlException("Error while writing " + configFile + ": " + e, e);
        }

    }

    public void addCluster(String key, Cluster cluster) {
        clusters.put(key, cluster);
    }

    public Cluster getCluster(String key) {
        return clusters.get(key);
    }

    @Override
    public Map<String, Object> toMap() {
        return clusters.entrySet().stream().map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toMap()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static class Cluster implements Document {
        private String server;
        private int port;
        private TLSConfig tlsConfig;

        public Cluster(String server, int port, TLSConfig tlsConfig) {
            this.server = server;
            this.port = port;
            this.tlsConfig = tlsConfig;
        }

        private Cluster() {

        }

        public static Cluster parse(Map<String, Object> config) throws ConfigValidationException {
            ValidationErrors validationErrors = new ValidationErrors();
            ValidatingDocNode vNode = new ValidatingDocNode(config, validationErrors).expandVariables("file", ConfigVariableProviders.FILE);

            Cluster result = new Cluster();

            result.server = vNode.get("server").required().asString();
            result.port = vNode.get("port").withDefault(9300).asInt();
            result.tlsConfig = vNode.get("tls").by(TLSConfig::parse);

            validationErrors.throwExceptionForPresentErrors();

            return result;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("server", server);
            result.put("port", port);
            result.put("tls", tlsConfig.toMap());

            return result;
        }

        public String getServer() {
            return server;
        }

        public int getPort() {
            return port;
        }

        public TLSConfig getTlsConfig() {
            return tlsConfig;
        }
    }

}
