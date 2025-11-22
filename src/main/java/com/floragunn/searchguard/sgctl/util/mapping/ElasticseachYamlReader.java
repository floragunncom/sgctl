package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.Map;

public class ElasticseachYamlReader extends ConfigReader {

    public ElasticseachYamlReader(File configFile, IntermediateRepresentation ir) {
        super(configFile, ir);
    }

    public void toIR(Map<String, Object> map) {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            // filter out every non-xpack.security option
            if (!key.startsWith("xpack.security.")) {
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
