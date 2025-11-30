package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRolesMapping;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import java.util.*;

public class RoleMappingsMigrator {

  // Collect unique users and roles
  private static class SgMappingBuilder {
    Set<String> users = new HashSet<>();
    Set<String> backendRoles = new HashSet<>();
  }

  public SgInternalRolesMapping convert(RoleMappings xpackRoleMappings) {
    Map<String, SgMappingBuilder> builderMap = new HashMap<>();

    for (Map.Entry<String, RoleMappings.RoleMapping> entry :
        xpackRoleMappings.mappings().entrySet()) {
      String mappingName = entry.getKey();
      RoleMappings.RoleMapping mapping = entry.getValue();

      if (mapping instanceof RoleMappings.RoleMapping.Templates) {
        // TODO: Implement logic (if that's even possible)
        continue;
      }

      if (mapping instanceof RoleMappings.RoleMapping.Roles rolesMapping) {
        if (!rolesMapping.enabled()) {
          continue;
        }

        // Recursively extract a flat list from the rules tree
        ExtractionResult extracted = extractIdentities(rolesMapping.rules(), mappingName);

        if (extracted.isEmpty()) {
          System.out.println("INFO [" + mappingName + "]: No migratable users/roles found!");
          continue;
        }

        for (String roleName : rolesMapping.roles()) {
          builderMap.computeIfAbsent(roleName, k -> new SgMappingBuilder());
          SgMappingBuilder builder = builderMap.get(roleName);

          builder.users.addAll(extracted.users);
          builder.backendRoles.addAll(extracted.backendRoles);
        }
      }
    }

    // Convert mutable builder to immutable SearchGuard object
    ImmutableMap.Builder<String, SgInternalRolesMapping.RoleMapping> finalMap =
        new ImmutableMap.Builder<>();

    for (Map.Entry<String, SgMappingBuilder> entry : builderMap.entrySet()) {
      SgMappingBuilder b = entry.getValue();

      SgInternalRolesMapping.RoleMapping sgMapping =
          new SgInternalRolesMapping.RoleMapping(
              ImmutableList.of(b.users),
              ImmutableList.of(b.backendRoles),
              ImmutableList.empty(),
              ImmutableList.empty());

      finalMap.put(entry.getKey(), sgMapping);
    }

    return new SgInternalRolesMapping(finalMap.build());
  }

  // Internal Logic

  private record ExtractionResult(List<String> users, List<String> backendRoles) {
    boolean isEmpty() {
      return users.isEmpty() && backendRoles.isEmpty();
    }
  }

  private ExtractionResult extractIdentities(
      RoleMappings.RoleMapping.Rule rule, String mappingName) {
    List<String> users = new ArrayList<>();
    List<String> backendRoles = new ArrayList<>();

    if (rule instanceof RoleMappings.RoleMapping.Rule.Any anyRule) {
      for (RoleMappings.RoleMapping.Rule subRule : anyRule.rules()) {
        ExtractionResult subResult = extractIdentities(subRule, mappingName);
        users.addAll(subResult.users);
        backendRoles.addAll(subResult.backendRoles);
      }

    } else if (rule instanceof RoleMappings.RoleMapping.Rule.All allRule) {
      // TODO: (If possible) Implement way to go from AND to OR logic without security problems
      System.err.println(
          "ERROR ["
              + mappingName
              + "]: Rule contains 'ALL' (AND logic). SKIPPED for security reasons.");

    } else if (rule instanceof RoleMappings.RoleMapping.Rule.Field fieldRule) {
      DocNode data = fieldRule.data();

      // Extract data from Field Rules
      if (data.hasNonNull("username")) {
        parseStringOrList(data.get("username"), users);
      } else if (data.hasNonNull("dn")) {
        parseStringOrList(data.get("dn"), users);
      } else if (data.hasNonNull("groups")) {
        parseStringOrList(data.get("groups"), backendRoles);
      } else if (data.hasNonNull("realm.name")) {
        // Haven't found a way to implement realms in SearchGuard (yet?)
        System.out.println("INFO [" + mappingName + "]: Ignoring 'realm.name' rule.");
      } else {
        System.out.println(
            "WARNING [" + mappingName + "]: Unknown field in rule: " + data.toJsonString());
      }
      // Haven't found a way yet to automatically translate negations to SearchGuard (positive only)
    } else if (rule instanceof RoleMappings.RoleMapping.Rule.Except exceptRule) {
      System.out.println("INFO [" + mappingName + "]: Ignoring negation.");
    }

    return new ExtractionResult(users, backendRoles);
  }

  // Needed, because field can be a String or List
  private void parseStringOrList(Object node, List<String> target) {

    if (node == null) {
      return;
    }

    if (node instanceof List) {
      for (Object item : (List<?>) node) {
        target.add(item.toString());
      }
    } else {
      target.add(node.toString());
    }
  }
}
