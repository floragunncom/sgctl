package com.floragunn.searchguard.sgctl.config.migrator;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.MigratorRegistry;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.trace.OptTraceable;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class MigratorTest {
  record NullMigrationContext() implements Migrator.IMigrationContext {
    @Override
    public Optional<RoleMappings> getRoleMappings() {
      return Optional.empty();
    }

    @Override
    public Optional<Roles> getRoles() {
      return Optional.empty();
    }

    @Override
    public Optional<Users> getUsers() {
      return Optional.empty();
    }

    @Override
    public Optional<XPackElasticsearchConfig> getElasticsearch() {
      return Optional.empty();
    }

    @Override
    public Optional<?> getKibana() {
      return Optional.empty();
    }
  }

  static class TestMigratorUsers implements SubMigrator {

    public static class SgInternalUsersConfig implements NamedConfig<SgInternalUsersConfig> {
      @Override
      public Object toBasicObject() {
        return Map.of("users", List.of("admin", "cthon98", "AzureDiamond"));
      }

      @Override
      public String getFileName() {
        return "sg_internal_users.yml";
      }
    }

    @Override
    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger) {
      logger.debug("TestMigratorUsers migrate start");

      final SgInternalUsersConfig sgInternalUsersConfig = new SgInternalUsersConfig();
      final List<NamedConfig<?>> namedConfigs = List.of(sgInternalUsersConfig);
      return namedConfigs;
    }
  }

  static class TestMigratorCombined implements SubMigrator {

    public static class SgInternalUsersConfig implements NamedConfig<SgInternalUsersConfig> {
      @Override
      public Object toBasicObject() {
        return Map.of("users", List.of("Bumbo", "Schr3in3r"));
      }

      @Override
      public String getFileName() {
        return "sg_internal_users.yml";
      }
    }

    public static class SgKibanaConfig implements NamedConfig<SgInternalUsersConfig> {
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
    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger) {
      logger.debug("TestMigratorCombined migrate start");

      final SgInternalUsersConfig sgInternalUsersConfig = new SgInternalUsersConfig();
      final SgKibanaConfig sgKibanaConfig = new SgKibanaConfig();
      final List<NamedConfig<?>> namedConfigs = List.of(sgInternalUsersConfig, sgKibanaConfig);
      return namedConfigs;
    }
  }

  static class FailingTestMigrator implements SubMigrator {

    @Override
    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
        throws SgctlException {
      logger.debug("FailingTestMigrator migrate start");
      throw new SgctlException("Failed to migrate because of ...");
    }
  }

  static class ReportingTestMigrator implements SubMigrator {

    Traceable<?> dummyTraceable1 = OptTraceable.empty(Source.NONE);
    Traceable<?> dummyTraceable2 = OptTraceable.empty(Source.NONE);

    @Override
    public List<NamedConfig<?>> migrate(
        Migrator.IMigrationContext context, MigrationReporter reporter) {
      reporter.problem(dummyTraceable1, "1");
      reporter.problem(dummyTraceable2, "2");
      reporter.inconvertible(dummyTraceable1, "1.1");
      reporter.inconvertible(dummyTraceable1, "1.2");
      reporter.generic("generic 1");
      reporter.generic("generic 2");
      return List.of();
    }
  }

  @Test
  public void testMigrationReporting() throws SgctlException {
    var test = new ReportingTestMigrator();
    var registry = MigratorRegistry.getInstance();
    registry.registerSubMigrator(test);
    registry.finalizeSubMigrators();

    var reporter = new AssertableMigrationReporter();
    new Migrator(reporter).migrate(new NullMigrationContext());

    reporter.assertProblem(test.dummyTraceable1);
    reporter.assertProblem(test.dummyTraceable2, "2");
    assertThrows(AssertionError.class, () -> reporter.assertProblem(test.dummyTraceable1));
    assertThrows(AssertionError.class, () -> reporter.assertProblem(test.dummyTraceable2));
    reporter.assertNoProblem(test.dummyTraceable1);
    reporter.assertNoProblem(test.dummyTraceable2);

    reporter.assertInconvertible(test.dummyTraceable1, "1.1");
    assertThrows(AssertionError.class, () -> reporter.assertNoInconvertible(test.dummyTraceable1));
    reporter.assertInconvertible(test.dummyTraceable1, "1.2");
    assertThrows(AssertionError.class, () -> reporter.assertInconvertible(test.dummyTraceable1));
    reporter.assertNoInconvertible(test.dummyTraceable1);

    reporter.assertGeneric("generic 1");
    reporter.assertGeneric("generic 2");

    reporter.assertNoMoreProblems();
  }

  @Test
  public void testMigrationSimple() throws SgctlException {
    var registry = MigratorRegistry.getInstance();
    // Register all sub-migrators
    registry.registerSubMigrator(new TestMigratorUsers());
    // Finalize to prevent error
    registry.finalizeSubMigrators();

    final Migrator migrator = new Migrator();

    // Do the migration
    NullMigrationContext context = new NullMigrationContext();
    final List<NamedConfig<?>> migrationResult;
    try {
      migrationResult = migrator.migrate(context).configs();
    } catch (IllegalStateException e) {
      System.err.println("MigratorA migrate failed. Did you forget to finalize?");
      throw e;
    }

    // ==== Testing the outputed migration ====

    // Migration should have worked
    assertNotNull(migrationResult);
    // We expect one sg_internal_users.yml output file
    assertEquals(1, migrationResult.size());

    // Check coherence of output config
    final var config = migrationResult.get(0);
    assertEquals("sg_internal_users.yml", config.getFileName());
    // Also check for exact class, because theoretically a different class could also provide
    // sg_internal_users.yml
    final var basicObject =
        getBasicObjectFromNamedConfigAndCheckForExpectedClassAndNotNull(
            config, TestMigratorUsers.SgInternalUsersConfig.class);
    // Check innards
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

  private static Object getBasicObjectFromNamedConfigAndCheckForExpectedClassAndNotNull(
      NamedConfig<?> config, Class<?> expectedClass) {
    if (config.getClass() == expectedClass) {
      // all ok
    } else {
      throw new IllegalStateException(
          String.format(
              "Output config has wrong class (%s), should be %s",
              config.getClass().getName(), TestMigratorUsers.class.getName()));
    }

    final var basicObject = config.toBasicObject();
    assertNotNull(basicObject);
    return basicObject;
  }

  @Test
  public void testMigrationFailureSameFileTwiceAndSameSubMigratorTwice() throws SgctlException {
    var registry = MigratorRegistry.getInstance();
    // Register sub-migrators
    registry.registerSubMigrator(new TestMigratorUsers());
    registry.registerSubMigrator(new TestMigratorUsers());
    // Finalize to prevent error
    registry.finalizeSubMigrators();

    final Migrator migrator = new Migrator();

    // Do the migration
    NullMigrationContext context = new NullMigrationContext();
    assertThrows(IllegalStateException.class, () -> migrator.migrate(context));
  }

  @Test
  public void testMigrationFailureSameFileTwiceMultipleDifferentSubMigrators()
      throws SgctlException {
    var registry = MigratorRegistry.getInstance();
    // Register sub-migrators
    registry.registerSubMigrator(new TestMigratorUsers());
    registry.registerSubMigrator(new TestMigratorCombined());
    // Finalize to prevent error
    registry.finalizeSubMigrators();

    final Migrator migrator = new Migrator();

    // Do the migration
    NullMigrationContext context = new NullMigrationContext();
    assertThrows(IllegalStateException.class, () -> migrator.migrate(context));
  }

  @Test
  public void testMigrationComplex() throws SgctlException {
    var registry = MigratorRegistry.getInstance();
    // Register sub-migrators
    registry.registerSubMigrator(new TestMigratorCombined());
    // Finalize to prevent error
    registry.finalizeSubMigrators();

    final Migrator migrator = new Migrator();

    // Do the migration
    NullMigrationContext context = new NullMigrationContext();
    final List<NamedConfig<?>> migrationResult;
    try {
      migrationResult = migrator.migrate(context).configs();
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
    assertEquals("sg_internal_users.yml", config0.getFileName());
    final var config1 = migrationResult.get(1);
    assertEquals("kibana.yml", config1.getFileName());

    final var convertedConfig0 =
        getBasicObjectFromNamedConfigAndCheckForExpectedClassAndNotNull(
            config0, TestMigratorCombined.SgInternalUsersConfig.class);
    final var convertedConfig1 =
        getBasicObjectFromNamedConfigAndCheckForExpectedClassAndNotNull(
            config1, TestMigratorCombined.SgKibanaConfig.class);

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

  @Test
  public void testFailingMigration() {
    var registry = MigratorRegistry.getInstance();
    // Register sub-migrators
    registry.registerSubMigrator(new FailingTestMigrator());
    // Finalize to prevent error
    registry.finalizeSubMigrators();

    final Migrator migrator = new Migrator();

    // Do the migration
    NullMigrationContext context = new NullMigrationContext();
    assertThrows(SgctlException.class, () -> migrator.migrate(context));
  }

  @AfterEach
  public void testCleanup() {
    // Needs to be called after every test, or they will fail
    final MigratorRegistry migratorRegistry = MigratorRegistry.getInstance();
    migratorRegistry.reset();
  }
}
