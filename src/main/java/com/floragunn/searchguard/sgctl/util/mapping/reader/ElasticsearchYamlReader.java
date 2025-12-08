package com.floragunn.searchguard.sgctl.util.mapping.reader;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
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

        String globalPrefix = "xpack.security.";
        String transportPrefix = "xpack.security.transport.ssl.";
        String httpPrefix = "xpack.security.http.ssl.";
        String sslTlsPrefix = "transport.";
        String authenticationPrefix = "xpack.security.authc.";

        // list all options that are skipped
        List<String> metadata = Arrays.asList(
                "cluster", "node", "bootstrap", "network", "discovery", "action", "path", "http", "transport",
                "indices", "gateway", "thread_pool", "processors", "plugins", "repositories", "monitoring", "xpack.ml",
                "xpack.monitoring", "xoack.enrich", "xpack.watcher", "xpack.license", "xpack.ilm", "xpack.slm",
                "xpack.data_frame", "logger", "log4j", "logging", "ingest", "script", "search", "snapshot", "xpack.snapshot",
                "cache", "memory", "xpack.fleet", "xpack.transform", "xpack.rollup", "xpack.sql", "xpack.searchable_snapshots",
                "xpack.voting_only", "xpack.ccr", "reindex",  "rest"
        );

        String stripped;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // check if metadata
            for (String meta : metadata) {
                if (key.startsWith(meta)) {
                    MigrationReport.shared.addIgnoredKey("elasticsearch.yml", key, key);
                }
            }

            // for each option name, propagate to responsible ir class method, added prefixes and file because they are required by the report api
            // Order matters
            if ((stripped = stripPrefix(key, transportPrefix)) != null) {
                ir.getSslTls().getTransport().handleTlsOptions(stripped, value, transportPrefix, configFile);
            } else if ((stripped = stripPrefix(key, httpPrefix)) != null) {
                ir.getSslTls().getHttp().handleTlsOptions(stripped, value, httpPrefix, configFile);
            } else if ((stripped = stripPrefix(key, sslTlsPrefix)) != null) {
                ir.getSslTls().handleOptions(stripped, value, sslTlsPrefix, configFile);
            } else if ((stripped = stripPrefix(key, authenticationPrefix)) != null) {
                ir.getAuthent().handleOptions(stripped, value, authenticationPrefix, configFile);
            } else if ((stripped = stripPrefix(key, globalPrefix)) != null) {
                ir.getGlobal().handleGlobalOptions(stripped, value, globalPrefix, configFile);
            }

            else {
                MigrationReport.shared.addUnknownKey("elasticsearch.yml", key, key);
            }
        }
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
