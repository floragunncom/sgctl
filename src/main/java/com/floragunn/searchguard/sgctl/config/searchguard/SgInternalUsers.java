package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import java.util.LinkedHashMap;
import java.util.Objects;

public record SgInternalUsers(ImmutableList<User> users) implements NamedConfig<SgInternalUsers> {

    public SgInternalUsers {
        Objects.requireNonNull(users, "users must not be null");
    }

    @Override
    public String getFileName() {
        return "sg_internal_users.yml";
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