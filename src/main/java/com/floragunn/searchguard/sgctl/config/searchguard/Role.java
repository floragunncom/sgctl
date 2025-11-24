package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Role(
        ImmutableList<String> cluster,
        ImmutableList<Permission> index,
        ImmutableList<Permission> alias,
        ImmutableList<Permission> dataStream,
        ImmutableList<Permission> tenant) {

    public record Permission(ImmutableList<String> patterns, ImmutableList<String> allowedActions) {
        public Map<String, Object> toMap(String prefix) {
            return ImmutableMap.of(prefix + "_patterns", patterns,
                    prefix + "_actions", allowedActions);
        }
    }

    private static List<Map<String, Object>> mapPermissions(List<Permission> perms, String prefix) {
        return perms.stream()
                .map(p -> p.toMap(prefix))
                .toList();
    }

    public Map<String, Object> toBasicObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cluster_permissions", cluster);
        if (!index.isEmpty()) result.put("index_permissions", mapPermissions(index, "index"));
        if (!alias.isEmpty()) result.put("alias_permissions", mapPermissions(alias, "alias"));
        if (!dataStream.isEmpty()) result.put("data_stream_permissions", mapPermissions(dataStream, "data_stream"));
        if (!tenant.isEmpty()) result.put("tenant_permissions", mapPermissions(tenant, "tenant"));
        return result;
    }
}
