package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigReader {

    private final File configFile;
    private IntermediateRepresentation ir;

    public ConfigReader(File configFile, IntermediateRepresentation ir) {
        this.configFile = configFile;
        this.ir = ir;
    }

    // read a config and return its contents as a map
    @SuppressWarnings("unchecked")
    public Map<String, Object> read() throws Exception {
        // the read method returns indeed a Map
        return (Map<String, Object>) DocReader.yaml().read(configFile);
    }

    // convert the XPack config into its corresponding IR, use the flattened map!
    public abstract void toIR(Map<String, Object> map);

    // flatten the parsed map, but keep all namespaces
    public static Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        flattenMapRec(map, result, "");
        return result;
    }

    private static void flattenMapRec(Object value, Map<String, Object> result, String prefix) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            map.forEach((k, v) -> {
                String newKey = prefix.isEmpty() ? k.toString() : prefix + "." + k;
                flattenMapRec(v, result, newKey);
            });
        } else {
            result.put(prefix, value);
        }
    }
}


