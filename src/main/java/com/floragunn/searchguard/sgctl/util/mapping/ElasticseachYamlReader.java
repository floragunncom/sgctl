package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.Map;

public class ElasticseachYamlReader extends ConfigReader {

    public ElasticseachYamlReader(File configFile, IntermediateRepresentation ir) {
        super(configFile, ir);
    }

    public void toIR(Map<String, Object> map) {

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
