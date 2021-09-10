package com.floragunn.searchguard.sgctl.client.api;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import com.floragunn.codova.documents.DocNode;
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
            this.content = docNode.getAsNode("content");
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
