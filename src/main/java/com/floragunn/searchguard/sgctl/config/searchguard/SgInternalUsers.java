package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

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

    public record User(String username, String password, ImmutableList<String> roles, ImmutableMap<String, String> metadata) {

        public User {
            Objects.requireNonNull(username, "username must not be null");
            Objects.requireNonNull(password, "password must not be null");
            Objects.requireNonNull(roles, "roles must not be null");
            Objects.requireNonNull(metadata, "metadata must not be null");
        }

        public Object toBasicObject() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("hash", password);
            result.put("opendistro_security_roles", roles);
            result.put("attributes", metadata);
            return result;
        }
    }

}