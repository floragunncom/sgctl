package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import java.util.List;

/** A sub part for the Migrator (SubMigrator) */
public interface SubMigrator {

  /**
   * @param context Contains all the parsed X-Pack configs
   * @param reporter For generating the migration report
   * @return Migrated Search Guard configs
   */
  List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, MigrationReporter reporter);
}
