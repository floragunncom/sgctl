package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocReader;

import java.io.File;
import java.util.Map;

public class ElasticseachYamlReader implements ConfigReader {

    private final File configFile;

    public ElasticseachYamlReader(File configFile) {
        this.configFile = configFile;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> read() throws Exception {
        // the read method returns indeed a Map
        return (Map<String, Object>) DocReader.yaml().read(configFile);
    }

    @Override
    public String toString() {
        String out = null;
        try {
            out = read().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

}
