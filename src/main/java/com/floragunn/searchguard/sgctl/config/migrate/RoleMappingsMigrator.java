package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRolesMapping;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import java.util.*;
import org.slf4j.Logger;

public class RoleMappingsMigrator implements SubMigrator {

  // Collect unique users and roles
  private static class SgMappingBuilder {
    Set<String> users = new HashSet<>();
    Set<String> backendRoles = new HashSet<>();
    Set<String> hosts = new HashSet<>();
    Set<String> ips = new HashSet<>();
  }

  @Override
  public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger) {
    if (context.getRoleMappings().isEmpty()) {
      logger.info("No X-Pack role mappings found. Skipping migration.");
      return List.of();
    }

    RoleMappings source = context.getRoleMappings().get();

    SgInternalRolesMapping result = convert(source, logger);

    return List.of(result);
  }

  private SgInternalRolesMapping convert(RoleMappings xpackRoleMappings, Logger logger) {
    Map<String, SgMappingBuilder> builderMap = new HashMap<>();

    for (Map.Entry<String, RoleMappings.RoleMapping> entry :
        xpackRoleMappings.mappings().entrySet()) {
      String mappingName = entry.getKey();
      RoleMappings.RoleMapping mapping = entry.getValue();

      if (mapping instanceof RoleMappings.RoleMapping.Templates) {
        logger.warn(
            "[{}] Skipping 'Role Templates'. Dynamic logic cannot be migrated to static YAML.",
            mappingName);
        continue;
      }

      if (mapping instanceof RoleMappings.RoleMapping.Roles rolesMapping) {
        if (!rolesMapping.enabled()) {
          continue;
        }

        // Recursively extract a flat list from the rules tree
        ExtractionResult extracted = extractIdentities(rolesMapping.rules(), mappingName, logger);

        if (extracted.isEmpty()) {
          logger.info("[{}] No migratable users/roles/hosts/ips found.", mappingName);
          continue;
        }

        for (String roleName : rolesMapping.roles()) {
          builderMap.computeIfAbsent(roleName, k -> new SgMappingBuilder());
          SgMappingBuilder builder = builderMap.get(roleName);

          builder.users.addAll(extracted.users);
          builder.backendRoles.addAll(extracted.backendRoles);
          builder.hosts.addAll(extracted.hosts);
          builder.ips.addAll(extracted.ips);
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
              ImmutableList.of(b.hosts),
              ImmutableList.of(b.ips));

      finalMap.put(entry.getKey(), sgMapping);
    }

    return new SgInternalRolesMapping(finalMap.build());
  }

  // Internal Logic

  private record ExtractionResult(
      List<String> users, List<String> backendRoles, List<String> hosts, List<String> ips) {
    boolean isEmpty() {
      return users.isEmpty() && backendRoles.isEmpty() && hosts.isEmpty() && ips.isEmpty();
    }
  }

  private ExtractionResult extractIdentities(
      RoleMappings.RoleMapping.Rule rule, String mappingName, Logger logger) {
    List<String> users = new ArrayList<>();
    List<String> backendRoles = new ArrayList<>();
    List<String> hosts = new ArrayList<>();
    List<String> ips = new ArrayList<>();

    if (rule instanceof RoleMappings.RoleMapping.Rule.Any anyRule) {
      for (RoleMappings.RoleMapping.Rule subRule : anyRule.rules()) {
        ExtractionResult subResult = extractIdentities(subRule, mappingName, logger);
        users.addAll(subResult.users);
        backendRoles.addAll(subResult.backendRoles);
        hosts.addAll(subResult.hosts);
        ips.addAll(subResult.ips);
      }

    } else if (rule instanceof RoleMappings.RoleMapping.Rule.All allRule) {
      logger.warn(
          "[{}] Rule contains 'ALL' (AND logic). Skipped for security reasons.", mappingName);
    } else if (rule instanceof RoleMappings.RoleMapping.Rule.Field fieldRule) {
      DocNode data = fieldRule.data();

      // Extract data from Field Rules
      if (data.hasNonNull("username")) {
        parseStringOrList(data.get("username"), users);
      } else if (data.hasNonNull("dn")) {
        parseStringOrList(data.get("dn"), users);
      } else if (data.hasNonNull("groups")) {
        parseStringOrList(data.get("groups"), backendRoles);
      } else if (data.hasNonNull("host")) {
        parseStringOrList(data.get("host"), hosts);
      } else if (data.hasNonNull("remote_ip")) {
        parseStringOrList(data.get("remote_ip"), ips);
      } else if (data.hasNonNull("realm.name")) {
        logger.warn("[{}] Ignoring 'realm.name' rule.", mappingName);
      } else {
        logger.warn("[{}] Unknown field in rule: {}", mappingName, data.toJsonString());
      }
    } else if (rule instanceof RoleMappings.RoleMapping.Rule.Except exceptRule) {
      logger.warn(
          "[{}] Rule contains 'EXCEPT' (Negation). Skipped for security reasons.", mappingName);
    }

    return new ExtractionResult(users, backendRoles, hosts, ips);
  }

  // Needed, because field can be a String or List
  private void parseStringOrList(Object node, List<String> target) {
    if (node == null) {
      return;
    }

    if (node instanceof Collection<?>) {
      for (Object item : (Collection<?>) node) {
        if (item != null) {
          target.add(String.valueOf(item));
        }
      }
    } else {
      target.add(String.valueOf(node));
    }
  }
}
