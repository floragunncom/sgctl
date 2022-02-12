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

package com.floragunn.searchguard.sgctl.client.api;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.google.common.collect.Iterators;

public class GetBulkConfigResponse implements Iterable<GetBulkConfigResponse.ConfigDocument> {

    private Map<ConfigType, ConfigDocument> configMap = new EnumMap<>(ConfigType.class);

    public GetBulkConfigResponse(DocNode docNode) {
        for (Map.Entry<String, Object> entry : docNode.entrySet()) {
            ConfigType configType;

            try {
                configType = ConfigType.get(entry.getKey());
            } catch (IllegalArgumentException e) {
                System.err.println("Got unknown config type " + entry.getKey() + " from API");
                continue;
            }

            configMap.put(configType, new ConfigDocument(configType, DocNode.wrap(entry.getValue())));
        }
    }

    public GetBulkConfigResponse(SearchGuardRestClient.Response response) throws InvalidResponseException {
        this(response.asDocNode());
    }

    public ConfigDocument get(ConfigType configType) {
        return configMap.get(configType);
    }

    @Override
    public Iterator<ConfigDocument> iterator() {
        return Iterators.unmodifiableIterator(configMap.values().iterator());
    }

    public static class ConfigDocument {
        private ConfigType configType;
        private DocNode content;

        public ConfigDocument(ConfigType configType, DocNode docNode) {
            this.content = docNode.hasNonNull("content") ? docNode.getAsNode("content") : DocNode.EMPTY;
            this.configType = configType;
        }

        public DocNode getContent() {
            return content;
        }

        public ConfigType getConfigType() {
            return configType;
        }
    }

}
