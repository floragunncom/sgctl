package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.util.Map;

public class ElasticseachYamlReader extends ConfigReader {

    public ElasticseachYamlReader(File configFile, IntermediateRepresentation ir) {
        super(configFile, ir);
    }

    public void toIR(Map<String, Object> map) {
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
