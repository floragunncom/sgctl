package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

public class RolesMigrator implements SubMigrator {

  HashMap<String, String> clusterPrivileges = clusterPrivileges();
  HashMap<String, String> indicesPriviliges = indicesPrivileges();

  public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
      throws SgctlException {
    logger.info("Migrating Roles");
    Optional<Roles> xpackRoles = context.getRoles();
    if (xpackRoles.isEmpty()) {
      logger.warn("roles.json is empty");
      return List.of();
    }
    // todo return
    return null;
  }

  private HashMap<String, String> clusterPrivileges() {
    HashMap<String, String> clusterPrivileges = new HashMap<>();
    clusterPrivileges.put("all", "SGS_CLUSTER_ALL");
    clusterPrivileges.put("createsnapshot", "SGS_MANAGE_SNAPSHOTS");
    clusterPrivileges.put("manageindextemplates", "SGS_CLUSTER_MANAGE_INDEX_TEMPLATES");
    clusterPrivileges.put("manageingestpipelines", "SGS_CLUSTER_MANAGE_PIPELINES");
    clusterPrivileges.put("monitor", "SGS_CLUSTER_MONITOR");
    return clusterPrivileges;
  }

  private HashMap<String, String> indicesPrivileges() {
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

  // todo names -> index pattern
  // todo priivileges -> allowed actions
  // todo renaming cluster privileges content
  /**
   * all -> SGS_CLUSTER_ALL createsnapshot -> SGS\MANAGE_SNAPSHOTS manageindextemplates ->
   * SGS_CLUSTER_MANAGE_INDEX_TEMPLATES manageingestpipelines -> SGS_CLUSTER_MANAGE_PIPELINES
   * monitor -> SGS_CLUSTER_MONITOR
   */

  // todo renaming indices privileges content
  /**
   * all -> SGS_INDICES_ALL create -> SGS_CREATE_INDEX createindex -> SGS_CREATE_INDEX delete ->
   * SGS_DELETE index -> SGS_WRITE manage -> SGS_MANAGE monitor -> SGS_INDICES_MONITOR read ->
   * SGS_READ write -> SGS_WRITE
   */
}
