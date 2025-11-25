package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ElasticseachYamlReader {

    private final File configFile;
    protected IntermediateRepresentation ir;
    Map<String, Object> flattenedMap;

    public ElasticseachYamlReader(File configFile, IntermediateRepresentation ir) {
        this.configFile = configFile;
        this.ir = ir;
        try {
            Map<String, Object> map = read();
            flattenedMap = flattenMap(map);
            toIR(flattenedMap);
            System.out.println(flattenedMap);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // read a config and return its contents as a map
    @SuppressWarnings("unchecked")
    public Map<String, Object> read() throws Exception {
        // the read method returns indeed a Map
        return (Map<String, Object>) DocReader.yaml().read(configFile);
    }

    // flatten the parsed map, but keep all namespaces
    private Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        flattenMapRec(map, result, "");
        return result;
    }

    private void flattenMapRec(Object value, Map<String, Object> result, String prefix) {
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

    private void toIR(Map<String, Object> map) {

        String transportPrefix = "xpack.security.transport.ssl.";
        String httpPrefix = "xpack.security.http.ssl.";

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // for each option name, propagate to responsible ir class method
            if (key.startsWith(transportPrefix)) {
                ir.sslTls.transport.handleTlsOptions(key.substring(transportPrefix.length()), value);
            } else if (key.startsWith(httpPrefix)) {
                ir.sslTls.http.handleTlsOptions(key.substring(httpPrefix.length()), value);
            }

            else {
                System.out.println("Could not resolve " + entry.getKey());
            }
        }

        return;
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
