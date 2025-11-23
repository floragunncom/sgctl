package com.floragunn.searchguard.sgctl.config.searchguard;

import java.util.LinkedHashMap;
import java.util.List;

public record User(String username, String password, List<String> roles, LinkedHashMap<String, String> metadata) {

    public Object toBasicObject() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("hash", password);
        result.put("opendistro_security_roles", roles);
        result.put("attributes", metadata);
        return result;
    }
}