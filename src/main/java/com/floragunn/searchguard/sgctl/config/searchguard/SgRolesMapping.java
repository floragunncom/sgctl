package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.Document;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record SgRolesMapping(ImmutableMap<String, Identities> mappings)
    implements NamedConfig<SgRolesMapping> {

  @Override
  public String getFileName() {
    return "sg_roles_mapping.yml";
  }

  // fields in this record may be empty, eg if the RoleMapping is configured only for specific
  // backend_roles
  // and user names aren't used for applying this role
  // fill with empty String "" if no rule was configured or found that fit
  public record Identities(
      ImmutableList<String> users,
      ImmutableList<String> backendRoles,
      ImmutableList<String> hosts,
      ImmutableList<String> ips)
      implements Document<Identities> {
    public Identities {
      Objects.requireNonNull(users, "users must not be NULL");
      Objects.requireNonNull(backendRoles, "backend_roles must not be NULL");
      Objects.requireNonNull(hosts, "hosts must not be NULL");
      Objects.requireNonNull(ips, "ips must not be NULL");
    }

    @Override
    public Map<String, Object> toBasicObject() {
      Map<String, Object> result = new LinkedHashMap<>();
      if (!users.isEmpty()) {
        result.put("users", users);
      }

      if (!backendRoles.isEmpty()) {
        result.put("backend_roles", backendRoles);
      }

      if (!hosts.isEmpty()) {
        result.put("hosts", hosts);
      }

      if (!ips.isEmpty()) {
        result.put("ips", ips);
      }
      return result;
    }
  }

  @Override
  public Object toBasicObject() {
    Map<String, Object> result = new LinkedHashMap<>();
    mappings.forEach((k, v) -> result.put(k, v.toBasicObject()));

    return result;
  }
}
