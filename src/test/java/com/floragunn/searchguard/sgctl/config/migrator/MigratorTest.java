package com.floragunn.searchguard.sgctl.config.migrator;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.MigratorRegistry;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class MigratorTest {

  static class TestMigratorUsers implements SubMigrator {

    public static class SgInteralUsersConfig implements NamedConfig<SgInteralUsersConfig> {
      @Override
      public Object toBasicObject() {
        return Map.of("users", List.of("admin", "cthon98", "AzureDiamond"));
      }

      @Override
      public String getFileName() {
        return "sg_interal_users.yml";
      }
    }

    @Override
    public List<NamedConfig<?>> migrate(Migrator.MigrationContext context, Logger logger) {
      logger.debug("TestMigratorUsers migrate start");

      final SgInteralUsersConfig sgInternalUsersConfig = new SgInteralUsersConfig();
      final List<NamedConfig<?>> namedConfigs = List.of(sgInternalUsersConfig);
      return namedConfigs;
    }
  }

  static class TestMigratorCombined implements SubMigrator {

    public static class SgInteralUsersConfig implements NamedConfig<SgInteralUsersConfig> {
      @Override
      public Object toBasicObject() {
        return Map.of("users", List.of("Bumbo", "Schr3in3r"));
      }

      @Override
      public String getFileName() {
        return "sg_interal_users.yml";
      }
    }

    public static class SgKibanaConfig implements NamedConfig<SgInteralUsersConfig> {
      @Override
      public Object toBasicObject() {
        return Map.of("setting", "null");
      }

      @Override
      public String getFileName() {
        return "kibana.yml";
      }
    }

    @Override
    public List<NamedConfig<?>> migrate(Migrator.MigrationContext context, Logger logger) {
      logger.debug("TestMigratorCombined migrate start");

      final SgInteralUsersConfig sgInternalUsersConfig = new SgInteralUsersConfig();
      final SgKibanaConfig sgKibanaConfig = new SgKibanaConfig();
      final List<NamedConfig<?>> namedConfigs = List.of(sgInternalUsersConfig, sgKibanaConfig);
      return namedConfigs;
    }
  }

  @Test
  public void testMigrationSimple() {
    // Register all sub-migrators
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorUsers());
    // Finalize to prevent error
    MigratorRegistry.finalizeMigratorsStatic();

    final Migrator migrator = new Migrator();

    // Do the migration
    Migrator.MigrationContext context = new Migrator.MigrationContext(null, null);
    final List<NamedConfig<?>> migrationResult;
    try {
      migrationResult = migrator.migrate(context);
    } catch (IllegalStateException e) {
      System.err.println("MigratorA migrate failed. Did you forget to finalize?");
      throw e;
    }

    // ==== Testing the outputed migration ====

    // Migration should have worked
    assertNotNull(migrationResult);
    // We expect one sg_interal_users.yml output file
    assertEquals(1, migrationResult.size());

    // Check coherence of output config
    final var config = migrationResult.get(0);
    assertEquals("sg_interal_users.yml", config.getFileName());
    // Also check for exact class, because theoretically a different class could also provide
    // sg_interal_users.yml
    if (config.getClass() == TestMigratorUsers.SgInteralUsersConfig.class) {
      // all ok
    } else {
      throw new IllegalStateException(
          String.format(
              "Output config has wrong class (%s), should be %s",
              config.getClass().getName(), TestMigratorUsers.class.getName()));
    }
    // Check innards
    final TestMigratorUsers.SgInteralUsersConfig testInteralUsersConfig =
        (TestMigratorUsers.SgInteralUsersConfig) config;
    final var basicObject = testInteralUsersConfig.toBasicObject();
    assertInstanceOf(Map.class, basicObject);
    final var configMap = (Map<?, ?>) basicObject;
    assertEquals(1, configMap.size());
    final var usersObject = configMap.get("users");
    assertNotNull(usersObject);
    assertInstanceOf(List.class, usersObject);
    final var users = (List<?>) usersObject;
    final var expectedUsers = List.of("admin", "cthon98", "AzureDiamond");
    assertEquals(expectedUsers.size(), users.size());
    for (int i = 0; i < expectedUsers.size(); i++) {
      final var expectedUser = expectedUsers.get(i);
      final var actualUser = users.get(i);
      assertInstanceOf(String.class, actualUser);
      assertEquals(expectedUser, actualUser);
    }
  }

  @Test
  public void testMigrationFailureSameFileTwice() {
    // Register sub-migrators
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorUsers());
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorUsers());
    // Finalize to prevent error
    MigratorRegistry.finalizeMigratorsStatic();

    final Migrator migrator = new Migrator();

    // Do the migration
    Migrator.MigrationContext context = new Migrator.MigrationContext(null, null);
    assertThrows(IllegalStateException.class, () -> migrator.migrate(context));
  }

  @Test
  public void testMigrationFailureSameFileTwiceMultipleDifferentSubMigrators() {
    // Register sub-migrators
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorUsers());
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorCombined());
    // Finalize to prevent error
    MigratorRegistry.finalizeMigratorsStatic();

    final Migrator migrator = new Migrator();

    // Do the migration
    Migrator.MigrationContext context = new Migrator.MigrationContext(null, null);
    assertThrows(IllegalStateException.class, () -> migrator.migrate(context));
  }

  @Test
  public void testMigrationComplex() {
    // Register sub-migrators
    MigratorRegistry.registerSubMigratorStatic(new TestMigratorCombined());
    // Finalize to prevent error
    MigratorRegistry.finalizeMigratorsStatic();

    final Migrator migrator = new Migrator();

    // Do the migration
    Migrator.MigrationContext context = new Migrator.MigrationContext(null, null);
    final List<NamedConfig<?>> migrationResult;
    try {
      migrationResult = migrator.migrate(context);
    } catch (IllegalStateException e) {
      System.err.println("TestMigratorCombined migrate failed. Did you forget to finalize?");
      throw e;
    }

    // ==== Testing the outputed migration ====

    // Migration should have worked
    assertNotNull(migrationResult);
    // We expect two output files
    assertEquals(2, migrationResult.size());

    // Check coherence of output config
    final var config0 = migrationResult.get(0);
    assertEquals("sg_interal_users.yml", config0.getFileName());
    final var config1 = migrationResult.get(1);
    assertEquals("kibana.yml", config1.getFileName());

    final var convertedConfig0 = config0.toBasicObject();
    final var convertedConfig1 = config1.toBasicObject();

    assertInstanceOf(Map.class, convertedConfig0);
    assertInstanceOf(Map.class, convertedConfig1);

    // Check config0
    final var convertedConfig0AsMap = (Map<?, ?>) convertedConfig0;
    assert (convertedConfig0AsMap.containsKey("users"));
    final var usersValue = convertedConfig0AsMap.get("users");
    assertNotNull(usersValue);
    assertEquals(List.of("Bumbo", "Schr3in3r"), usersValue);

    // Check config1
    final var convertedConfig1AsMap = (Map<?, ?>) convertedConfig1;
    assert (convertedConfig1AsMap.containsKey("setting"));
    final var settingValue = convertedConfig1AsMap.get("setting");
    assertNotNull(settingValue);
    assertEquals("null", settingValue);
  }
}
