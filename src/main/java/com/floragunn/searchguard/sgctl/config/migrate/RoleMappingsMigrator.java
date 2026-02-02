package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgRolesMapping;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class RoleMappingsMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {
    var roleMappings = context.getRoleMappings();
    if (roleMappings.isEmpty()) {
      reporter.problem("Skipping role-mappings migration: no role mappings json provided");
      return List.of();
    }

    var sgRoleMappings = new LinkedHashMap<String, SgRolesMapping.Identities>();
    for (var xPackMapping : roleMappings.get().mappings().get().values()) {
      for (var mappingEntry : migrateRoleMapping(xPackMapping, reporter).entrySet()) {
        var sgRoleName = mappingEntry.getKey();
        var identities = mappingEntry.getValue();
        sgRoleMappings.compute(
            sgRoleName,
            (x, otherIdentities) -> {
              if (otherIdentities == null) {
                return identities;
              } else {
                return merge(otherIdentities, identities);
              }
            });
      }
    }

    return List.of(new SgRolesMapping(ImmutableMap.of(sgRoleMappings)));
  }

  private ImmutableMap<String, SgRolesMapping.Identities> migrateRoleMapping(
      Traceable<RoleMappings.RoleMapping> mapping, MigrationReporter reporter) {

    if (!(mapping.get() instanceof RoleMappings.RoleMapping.Roles rolesMapping)) {
      reporter.inconvertible(mapping, "Template-based role mappings cannot be converted");
      return ImmutableMap.empty();
    }

    // disabled role mapping are just removed
    if (!rolesMapping.enabled().get()) return ImmutableMap.empty();

    var identities = extractIdentities(rolesMapping.rules(), reporter);
    if (identities.users().isEmpty()
        && identities.backendRoles().isEmpty()
        && identities.hosts().isEmpty()
        && identities.ips().isEmpty()) {
      reporter.problem(rolesMapping.rules(), "No migratable users/roles/hosts/ips found");
      return ImmutableMap.empty();
    }

    var builder = new ImmutableMap.Builder<String, SgRolesMapping.Identities>();
    for (var sgRoleName : rolesMapping.roles().get()) {
      builder.put(sgRoleName.get(), identities);
    }
    return builder.build();
  }

  private SgRolesMapping.Identities extractIdentities(
      Traceable<RoleMappings.RoleMapping.Rule> rule, MigrationReporter reporter) {
    var users = new ImmutableList.Builder<String>();
    var backendRoles = new ImmutableList.Builder<String>();
    var hosts = new ImmutableList.Builder<String>();
    var ips = new ImmutableList.Builder<String>();

    if (rule.get() instanceof RoleMappings.RoleMapping.Rule.Any anyRule) {
      for (var subRule : anyRule.rules().get()) {
        var subResult = extractIdentities(subRule, reporter);
        users.addAll(subResult.users());
        backendRoles.addAll(subResult.backendRoles());
        hosts.addAll(subResult.hosts());
        ips.addAll(subResult.ips());
      }
    } else if (rule.get() instanceof RoleMappings.RoleMapping.Rule.All) {
      reporter.critical(rule, "'ALL' (AND logic) rule has no equivalent.");
    } else if (rule.get() instanceof RoleMappings.RoleMapping.Rule.Field fieldRule) {
      var data = fieldRule.match();

      // Extract data from Field Rules
      if (data.get().containsKey("username")) {
        parseStringOrList(data.get().get("username"), users);
      } else if (data.get().containsKey("dn")) {
        parseStringOrList(data.get().get("dn"), users);
      } else if (data.get().containsKey("groups")) {
        parseStringOrList(data.get().get("groups"), backendRoles);
      } else if (data.get().containsKey("host")) {
        parseStringOrList(data.get().get("host"), hosts);
      } else if (data.get().containsKey("remote_ip")) {
        parseStringOrList(data.get().get("remote_ip"), ips);
      } else {
        reporter.problem(data, "Ignoring unknown field rule.");
      }
    } else if (rule.get() instanceof RoleMappings.RoleMapping.Rule.Except) {
      reporter.critical(rule, "'EXCEPT' (Negation) rule has no equivalent.");
    }

    return new SgRolesMapping.Identities(
        users.build(), backendRoles.build(), hosts.build(), ips.build());
  }

  // Needed, because field can be a String or List
  private void parseStringOrList(
      Traceable<Object> basicObject, ImmutableList.Builder<String> target) {
    if (basicObject.get() == null) return;

    if (basicObject.get() instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (item != null) target.add(String.valueOf(item));
      }
    } else {
      target.add(String.valueOf(basicObject.get()));
    }
  }

  private SgRolesMapping.Identities merge(
      SgRolesMapping.Identities a, SgRolesMapping.Identities b) {
    var users = new ImmutableList.Builder<String>();
    var backendRoles = new ImmutableList.Builder<String>();
    var hosts = new ImmutableList.Builder<String>();
    var ips = new ImmutableList.Builder<String>();

    users.addAll(a.users());
    users.addAll(b.users());
    backendRoles.addAll(a.backendRoles());
    backendRoles.addAll(b.backendRoles());
    hosts.addAll(a.hosts());
    hosts.addAll(b.hosts());
    ips.addAll(a.ips());
    ips.addAll(b.ips());

    return new SgRolesMapping.Identities(
        users.build(), backendRoles.build(), hosts.build(), ips.build());
  }
}
