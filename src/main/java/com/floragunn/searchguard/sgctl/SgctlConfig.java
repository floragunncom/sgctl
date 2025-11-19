/*
 * Copyright 2021 floragunn GmbH
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

package com.floragunn.searchguard.sgctl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.floragunn.codova.config.net.TLSConfig;
import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidatingFunction;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.VariableResolvers;
import com.floragunn.codova.validation.errors.JsonValidationError;

public class SgctlConfig {

    public static class Cluster implements Document<Cluster> {
        private String clusterId;
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

        public static Cluster read(File configDir, String clusterId) throws SgctlException {
            File configFile = new File(configDir, "cluster_" + clusterId + ".yml");

            try {

                if (!configFile.exists()) {
                    return null;
                }

                Map<String, Object> config;

                try {
                    config = DocReader.yaml().readObject(configFile);
                } catch (JsonProcessingException e) {
                    throw new ConfigValidationException(new JsonValidationError(null, e));
                } catch (IOException e) {
                    throw new SgctlException("Error while reading " + configFile + ": " + e, e);
                }

                return parse(config, clusterId);
            } catch (ConfigValidationException e) {
                throw new SgctlException("File " + configFile + " is invalid:\n" + e.getValidationErrors(), e).debugDetail(e.toDebugString());
            }
        }

        public void write(File configDir) throws SgctlException {
            if (!configDir.exists()) {
                if (!configDir.mkdir()) {
                    throw new SgctlException("Could not create directory " + configDir + ")");
                }
            }

            File configFile = new File(configDir, "cluster_" + clusterId + ".yml");

            try {
                DocWriter.yaml().write(configFile, this.toBasicObject());
            } catch (IOException e) {
                throw new SgctlException("Error while writing " + configFile + ": " + e, e);
            }

        }

        public static Cluster parse(Map<String, Object> config, String clusterId) throws ConfigValidationException {
            ValidationErrors validationErrors = new ValidationErrors();
            ValidatingDocNode vNode = new ValidatingDocNode(config, validationErrors).expandVariables("file", VariableResolvers.FILE);

            Cluster result = new Cluster();

            result.server = vNode.get("server").required().asString();
            result.port = vNode.get("port").withDefault(9300).asInt();
            result.tlsConfig = vNode.get("tls").required().by((ValidatingFunction<DocNode, TLSConfig>) TLSConfig::parse);
            result.clusterId = clusterId;

            validationErrors.throwExceptionForPresentErrors();

            return result;
        }

        @Override
        public Map<String, Object> toBasicObject() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("server", server);
            result.put("port", port);
            result.put("tls", tlsConfig.toBasicObject());

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

        public String getClusterId() {
            return clusterId;
        }

        public void setClusterId(String clusterId) {
            this.clusterId = clusterId;
        }
    }

}
