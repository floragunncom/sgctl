package com.floragunn.searchguard.sgctl.config.migrate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Format;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRolesMapping;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class RoleMappingsMigratorTest {

  private RoleMappingsMigrator migrator;
  private Migrator.IMigrationContext context;
  private Logger logger;

  @BeforeEach
  public void setUp() {
    migrator = new RoleMappingsMigrator();
    // Fake MigrationContext to control test input
    context = mock(Migrator.IMigrationContext.class);
    // Fake Logger to verify warning messages without real logging
    logger = mock(Logger.class);
  }

  @Test
  public void testFullMigration() throws IOException {

    String resourcePath = "/xpack_migrate/role_mapping/xpack_role_mappings.json";

    // Load and parse test data
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(is, "Couldn't find test file! Path: " + resourcePath);

      DocNode docNode = DocNode.parse(Format.JSON).from(is);
      RoleMappings xpackMappings = RoleMappings.parse(docNode, null);

      // Provide test data to the mocked context
      when(context.getRoleMappings()).thenReturn(Optional.of(xpackMappings));

    } catch (ConfigValidationException e) {
      throw new RuntimeException(e);
    }

    // Run migration
    List<NamedConfig<?>> resultList = migrator.migrate(context, logger);

    assertFalse(resultList.isEmpty(), "The result list cannot be empty.");

    // Get migration result and cast it
    SgInternalRolesMapping result = (SgInternalRolesMapping) resultList.get(0);

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

    // Verify that warning messages are logged correctly
    verify(logger, atLeastOnce()).warn(contains("ALL"), any(Object.class));
    verify(logger, atLeastOnce()).warn(contains("EXCEPT"), any(Object.class));
    verify(logger, atLeastOnce()).warn(contains("realm.name"), any(Object.class));
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
}
