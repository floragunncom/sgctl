package com.floragunn.searchguard.sgctl.config.migrate;

import static com.floragunn.searchguard.sgctl.testutil.TextAssertions.assertEqualsNormalized;
import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.searchguard.sgctl.config.migrator.AssertableMigrationReporter;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgRolesMapping;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class RoleMappingsMigratorTest {

  @Test
  public void testFullMigration() throws Exception {
    var roleMappings = loadRoleMappings("/xpack_migrate/role_mapping/xpack_role_mappings.json");
    var context = createContext(Optional.of(roleMappings));
    var migrator = new RoleMappingsMigrator();
    var reporter = new AssertableMigrationReporter();

    // Run migration
    List<NamedConfig<?>> resultList = migrator.migrate(context, reporter);

    assertFalse(resultList.isEmpty(), "The result list cannot be empty.");

    // Get migration result and cast it
    SgRolesMapping result = (SgRolesMapping) resultList.get(0);

    // Convert result into map for easier assertions
    Map<String, Object> outputMap = (Map<String, Object>) result.toBasicObject();

    // Validate migrated users, backend roles, IPs, hosts, and recursive role mappings
    assertTrue(outputMap.containsKey("sg_mixed_role"));
    Map<String, Object> mixedRole = (Map<String, Object>) outputMap.get("sg_mixed_role");

    List<String> users = getList(mixedRole, "users");
    List<String> roles = getList(mixedRole, "backend_roles");

    assertTrue(users.contains("hans"));
    assertTrue(users.contains("cn=boss,dc=corp"));
    assertTrue(roles.contains("admins"));
    assertTrue(roles.contains("devs"));

    assertTrue(outputMap.containsKey("sg_network_role"));
    Map<String, Object> netRole = (Map<String, Object>) outputMap.get("sg_network_role");

    List<String> ips = getList(netRole, "ips");
    List<String> hosts = getList(netRole, "hosts");

    assertTrue(ips.contains("10.0.0.1"));
    assertTrue(hosts.contains("office-pc-1"));

    assertFalse(
        outputMap.containsKey("sg_disabled_role"), "Disabled Mappings have been falsely migrated.");

    assertTrue(outputMap.containsKey("sg_recursive_role"));
    Map<String, Object> recursiveRole = (Map<String, Object>) outputMap.get("sg_recursive_role");
    List<String> recursiveUsers = getList(recursiveRole, "users");

    assertTrue(recursiveUsers.contains("nested_user"));
    assertTrue(recursiveUsers.contains("top_level_user"));

    if (outputMap.containsKey("sg_empty_role_1")) {
      Map<String, Object> m = (Map<String, Object>) outputMap.get("sg_empty_role_1");
      assertTrue(isListEmpty(m.get("users")), "Users list should be empty for ALL-Rules.");
    }

    // core problems
    reporter.assertCritical(
        "role_mappings.json: security_fail_all.rules", "'ALL' (AND logic) rule has no equivalent.");
    reporter.assertCritical(
        "role_mappings.json: security_fail_except.rules",
        "'EXCEPT' (Negation) rule has no equivalent.");
    reporter.assertProblem(
        "role_mappings.json: info_realm_ignored.rules.any.1.field", "Ignoring unknown field rule.");

    // problems because it cannot parse all / except
    reporter.assertProblem(
        "role_mappings.json: security_fail_all.rules", "No migratable users/roles/hosts/ips found");
    reporter.assertProblem(
        "role_mappings.json: security_fail_except.rules",
        "No migratable users/roles/hosts/ips found");

    reporter.assertNoMoreProblems();
  }

  @Test
  public void testRoleMappingsAreMerged() throws Exception {
    var roleMappings = loadRoleMappings("/xpack_migrate/role_mapping/mergeable_role_mappings.json");
    var expected =
        loadResourceAsString("/xpack_migrate/role_mapping/migrated/mergeable_role_mappings.yml");

    var context = createContext(Optional.of(roleMappings));
    var migrator = new RoleMappingsMigrator();
    var reporter = new AssertableMigrationReporter();

    var migrated = migrator.migrate(context, reporter).get(0);

    assertEqualsNormalized(expected, DocWriter.yaml().writeAsString(migrated));
    reporter.assertNoMoreProblems();
  }

  // Helper functions
  private List<String> getList(Map<String, Object> map, String key) {
    Object val = map.get(key);
    if (val instanceof List) {
      return (List<String>) val;
    }
    return List.of();
  }

  private boolean isListEmpty(Object val) {
    if (val == null) return true;
    if (val instanceof List) return ((List<?>) val).isEmpty();
    return true;
  }

  private RoleMappings loadRoleMappings(String path) throws Exception {
    var node = DocNode.wrap(DocReader.yaml().read(loadResourceAsString(path)));
    return RoleMappings.parse(node, Parser.Context.get());
  }

  private String loadResourceAsString(String path) throws Exception {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return new String(in.readAllBytes());
    }
  }

  private Migrator.MigrationContext createContext(Optional<RoleMappings> roleMappings) {
    return new Migrator.MigrationContext(
        roleMappings, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }
}
