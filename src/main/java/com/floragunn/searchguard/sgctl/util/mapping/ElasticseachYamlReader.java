package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocReader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ElasticseachYamlReader extends ConfigReader {

    public ElasticseachYamlReader(File configFile) {
        super(configFile);
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
