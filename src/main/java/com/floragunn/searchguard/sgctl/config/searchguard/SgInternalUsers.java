package com.floragunn.searchguard.sgctl.config.searchguard;

import java.util.LinkedHashMap;
import java.util.List;

public record SgInternalUsers(List<User> users) implements NamedConfig<Object> {

    @Override
    public String getFileName() {
        return "sg_internal_users";
    }

    @Override
    public Object toBasicObject() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for(User user : users) {
            result.put(user.username(), user.toBasicObject());
        }
        return result;
    }
}