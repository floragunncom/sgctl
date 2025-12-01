package com.floragunn.searchguard.sgctl.util.mapping;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;

public class ElasticsearchYamlReader {

    private final File configFile;
    protected IntermediateRepresentationElasticSearchYml ir;
    Map<String, Object> flattenedMap;

    public ElasticsearchYamlReader(File configFile, IntermediateRepresentationElasticSearchYml ir) {
        this.configFile = configFile;
        this.ir = ir;
        try {
            Map<String, Object> map = read();
            flattenedMap = flattenMap(map);
            toIR(flattenedMap);
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

    private static String stripPrefix(String key, String... prefixes) {
        for (String p : prefixes) {
            if (key.startsWith(p)) {
                return key.substring(p.length());
            }
        }
        return null;
    }

    private void toIR(Map<String, Object> map) {

        String[] globalPrefixes = {"xpack.security."};
        String[] transportPrefixes = {"xpack.security.transport.ssl."};
        String[] httpPrefixes = {"xpack.security.http.ssl."};
        String[] sslTlsPrefixes = {"transport."};
        String[] authenticationPrefixes = {"xpack.security.authc."};

        String stripped;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // for each option name, propagate to responsible ir class method
            if ((stripped = stripPrefix(key, transportPrefixes)) != null) {
                ir.sslTls.transport.handleTlsOptions(stripped, value);
            } else if ((stripped = stripPrefix(key, httpPrefixes)) != null) {
                ir.sslTls.http.handleTlsOptions(stripped, value);
            } else if ((stripped = stripPrefix(key, sslTlsPrefixes)) != null) {
                ir.sslTls.handleOptions(stripped, value);
            } else if ((stripped = stripPrefix(key, authenticationPrefixes)) != null) {
                ir.authent.handleOptions(stripped, value);
            } else if ((stripped = stripPrefix(key, globalPrefixes)) != null) {
                ir.global.handleGlobalOptions(stripped, value);
            }

            else {
                System.out.println("Could not resolve " + entry.getKey());
            }
        }

        return;
    }

    public static String getFieldsAsString(Object o) {
        String result = "";
        try {
            Class<?> c = o.getClass();
            for (var field : c.getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(o);
                result += field.getName() + ": " + val + '\n';
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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
