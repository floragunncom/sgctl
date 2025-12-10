package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;

/**
 * The result of the migration process.
 *
 * @param configs The migrated configurations.
 * @param report The migration report.
 */
public sealed interface MigrationResult {

  /**
   * @return The migration report
   */
  String report();

  /**
   * A successful migration result.
   *
   * @param configs The migrated configurations.
   * @param report The migration report.
   */
  record Success(ImmutableList<NamedConfig<?>> configs, String report) implements MigrationResult {}

  /**
   * A failed migration result.
   *
   * @param report The migration report.
   */
  record Failure(String report) implements MigrationResult {}
}
