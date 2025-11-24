package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

public record SgInternalRoles(ImmutableMap<String, Role> roles) implements NamedConfig<SgInternalRoles> {
    @Override
    public String getFileName() {
        return "sg_roles.yml";
    }

    @Override
    public Object toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        roles.forEach((k, v) -> result.put(k, v.toBasicObject()));
        return result;
    }
}

