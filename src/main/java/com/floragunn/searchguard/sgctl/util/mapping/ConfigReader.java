package com.floragunn.searchguard.sgctl.util.mapping;

import java.util.Map;

public interface ConfigReader {

    // read a config and return its contents as a map
    Map<String, Object> read() throws Exception;
}


