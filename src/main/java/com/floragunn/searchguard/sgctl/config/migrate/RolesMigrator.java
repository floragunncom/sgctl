package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRoles;
import com.floragunn.searchguard.sgctl.config.trace.OptTraceable;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the migrate method that Turns X-Pack roles into Searchguard roles by renaming parts of
 * the received roles Contains two maps with the corresponding X-Pack privileges and SG allowed
 * actions
 */
public class RolesMigrator implements SubMigrator {

  HashMap<String, String> clusterPrivileges = clusterPrivileges();
  HashMap<String, String> indicesPrivileges = indicesPrivileges();

  /**
   * Method that Reads out Roles and converts the X-Pack privileges into SG action groups
   *
   * @param context Contains all the parsed XPack configs
   * @param reporter MigrationReporter for the migration report
   * @return List<SgInternalRole>
   */
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    Optional<Roles> xpackRoles = context.getRoles();
    if (xpackRoles.isEmpty()) {
      reporter.problem("roles.json is empty");
      return List.of();
    }
    var internalRolesBuilder =
        new ImmutableMap.Builder<String, SgInternalRoles.Role>(xpackRoles.get().roles().size());

    // cluster -> translate straight, list of permissions
    // index -> index group
    //          allowed action

    for (Map.Entry<String, OptTraceable<Roles.Role>> entry : xpackRoles.get().roles().entrySet()) {

      var clusterBuilder =
          new ImmutableList.Builder<String>(entry.getValue().getValue().cluster().get().size());

      for (Traceable<String> cluster : entry.getValue().getValue().cluster().get()) {

        if (clusterPrivileges.containsKey(cluster.get())) {
          clusterBuilder.add(clusterPrivileges.get(cluster.get()));
        } else {
          reporter.inconvertible(
              cluster,
              "Could not migrate cluster privilege " + cluster + " for role " + entry.getKey());
        }
      }

      var indicesBuilder =
          new ImmutableList.Builder<SgInternalRoles.Role.Permission>(
              entry.getValue().getValue().indices().get().size());

      for (Traceable<Roles.Index> index : entry.getValue().getValue().indices().get()) {
        var actionsBuilder =
            new ImmutableList.Builder<String>(index.get().privileges().get().size());

        for (Traceable<String> privilege : index.get().privileges().get()) {
          if (indicesPrivileges.containsKey(privilege.get())) {
            actionsBuilder.add(indicesPrivileges.get(privilege.get()));
          } else {
            reporter.inconvertible(
                privilege,
                "Could not migrate index privilege " + privilege + " for role " + entry.getKey());
          }
        }

        // Convert name list of index from list of traceable strings to list of strings
        var indicesNamesBuilder =
            new ImmutableList.Builder<String>(index.get().names().get().size());
        for (Traceable<String> name : index.get().names().get())
          indicesNamesBuilder.add(name.get());

        SgInternalRoles.Role.Permission permission =
            new SgInternalRoles.Role.Permission(
                indicesNamesBuilder.build(), actionsBuilder.build());
        indicesBuilder.add(permission);
      }

      internalRolesBuilder.put(
          entry.getKey(),
          new SgInternalRoles.Role(
              clusterBuilder.build(),
              indicesBuilder.build(),
              ImmutableList.empty(),
              ImmutableList.empty(),
              ImmutableList.empty()));
    }

    reporter.problem("Alias permissions left empty.");
    reporter.problem("Data stream permissions left empty.");
    reporter.problem("Tenant permissions left empty.");

    return List.of(new SgInternalRoles(internalRolesBuilder.build()));
  }

  /**
   * A method to generate the map containing X-pack security cluster privileges into SG allowed
   * action groups
   *
   * @return HashMap<String, String>
   */
  private static HashMap<String, String> clusterPrivileges() {
    HashMap<String, String> clusterPrivileges = new HashMap<>();
    clusterPrivileges.put("all", "SGS_CLUSTER_ALL");
    clusterPrivileges.put("createsnapshot", "SGS_MANAGE_SNAPSHOTS");
    clusterPrivileges.put("manageindextemplates", "SGS_CLUSTER_MANAGE_INDEX_TEMPLATES");
    clusterPrivileges.put("manageingestpipelines", "SGS_CLUSTER_MANAGE_PIPELINES");
    clusterPrivileges.put("monitor", "SGS_CLUSTER_MONITOR");
    return clusterPrivileges;
  }

  /**
   * A method to generate the map containing X-pack security index privileges into SG allowed action
   * groups
   *
   * @return HashMap<String, String>
   */
  private static HashMap<String, String> indicesPrivileges() {
    HashMap<String, String> indicesPrivileges = new HashMap<>();
    indicesPrivileges.put("all", "SGS_INDEX_ALL");
    indicesPrivileges.put("createindex", "SGS_CREATE_INDEX");
    indicesPrivileges.put("create", "SGS_CREATE_INDEX");
    indicesPrivileges.put("delete", "SGS_DELETE");
    indicesPrivileges.put("index", "SGS_WRITE");
    indicesPrivileges.put("manage", "SGS_MANAGE");
    indicesPrivileges.put("monitor", "SGS_INDICES_MONITOR");
    indicesPrivileges.put("read", "SGS_READ");
    indicesPrivileges.put("write", "SGS_WRITE");
    return indicesPrivileges;
  }
}
