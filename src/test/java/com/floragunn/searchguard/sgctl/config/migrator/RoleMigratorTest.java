package com.floragunn.searchguard.sgctl.config.migrator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.floragunn.codova.documents.*;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.RolesMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRoles;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class RoleMigratorTest {

  private RolesMigrator migrator;
  private Migrator.IMigrationContext context;
  private Logger logger;

  @BeforeEach
  public void setUp() {
    migrator = new RolesMigrator();
    context = mock(Migrator.IMigrationContext.class);
    logger = mock(Logger.class);
  }

  // todo check correct replacement
  // todo check removal of unmigratable privileges
  // todo index privileges
  // todo check logger for unmigratable privileges
  // todo check for logger warning of empty roles file

  @Test
  public void testMigration() throws IOException, SgctlException {
    String resourcePath = "/xpack_migrate/roles/roles_test_cases_2.yml";

    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(is, "couldn't find test file Path:" + resourcePath);
      DocNode docNode = DocNode.parse(Format.YAML).from(is);

      Roles xPackRoles = Roles.parse(docNode, null);

      when(context.getRoles()).thenReturn(Optional.of(xPackRoles));
    } catch (ConfigValidationException e) {
      throw new RuntimeException(e);
    }

    List<NamedConfig<?>> resultList = migrator.migrate(context, logger);
    assertFalse(resultList.isEmpty(), "Migrator Output cannot be empty");

    SgInternalRoles parsedRoles = (SgInternalRoles) resultList.get(0);

    Map<String, Object> testRoles = (Map<String, Object>) parsedRoles.toBasicObject();

    assertTrue(testRoles.containsKey("test_role2"));

    Map<String, Object> testRole2 = (Map<String, Object>) testRoles.get("test_role2");

    List<String> clusterActionGroups = (List<String>) testRole2.get("cluster_permissions");
    List<Map<String, Object>> indexPermissions =
        (List<Map<String, Object>>) testRole2.get("index_permissions");
    Map<String, Object> testPermissions = indexPermissions.get(0);
    List<String> indexActionGroups = (List<String>) testPermissions.get("allowed_actions");

    assertTrue(clusterActionGroups.contains("SGS_CLUSTER_ALL"));
    assertTrue(clusterActionGroups.contains("SGS_MANAGE_SNAPSHOTS"));
    assertTrue(clusterActionGroups.contains("SGS_CLUSTER_MANAGE_INDEX_TEMPLATES"));
    assertTrue(clusterActionGroups.contains("SGS_CLUSTER_MANAGE_PIPELINES"));
    assertTrue(clusterActionGroups.contains("SGS_CLUSTER_MONITOR"));

    assertTrue(indexActionGroups.contains("SGS_INDEX_ALL"));
    assertTrue(indexActionGroups.contains("SGS_CREATE_INDEX"));
    assertTrue(indexActionGroups.contains("SGS_CREATE_INDEX"));
    assertTrue(indexActionGroups.contains("SGS_DELETE"));
    assertTrue(indexActionGroups.contains("SGS_WRITE"));
    assertTrue(indexActionGroups.contains("SGS_MANAGE"));
    assertTrue(indexActionGroups.contains("SGS_INDICES_MONITOR"));
    assertTrue(indexActionGroups.contains("SGS_READ"));
    assertTrue(indexActionGroups.contains("SGS_WRITE"));
    assertFalse(indexActionGroups.contains("typocorection"));

    verify(logger, atLeastOnce()).info("Migrating Roles");
    verify(logger, atLeastOnce())
        .warn("Could not migrate index privilege typocorection for role test_role2");
    verify(logger, atLeastOnce()).warn("Alias permissions left empty.");
    verify(logger, atLeastOnce()).warn("Data stream permissions left empty.");
    verify(logger, atLeastOnce()).warn("Tenant permissions left empty.");
  }
}
