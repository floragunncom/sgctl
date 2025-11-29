package com.floragunn.searchguard.sgctl.config.migrator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.MigratorRegistry;
import com.floragunn.searchguard.sgctl.config.migrate.SubMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigratorTest {
  @Test
  public void testMigration() {
    // TODO: Finish Test

    SubMigrator subMigrator =
        new SubMigrator() {
          @Override
          public List<NamedConfig<?>> migrate(Migrator.MigrationContext context, Logger logger) {
            return List.of();
          }
        };
    MigratorRegistry.registerSubMigratorStatic(subMigrator);
    MigratorRegistry.finalizeMigratorsStatic();

    Migrator migrator = new Migrator();

    Migrator.MigrationContext context = new Migrator.MigrationContext(null, null);

    migrator.migrate(context);

    assertTrue(true);
  }

  private static final Logger logger = LoggerFactory.getLogger(MigratorTest.class);
}
