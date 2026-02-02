package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;

/**
 * The result of the migration process.
 *
 * @param configs The migrated configurations.
 * @param report The migration report.
 * @param summary The migration report summary.
 */
public sealed interface MigrationResult {

  /**
   * @return The migration report
   */
  String report();

  /**
   * @return The migration report
   */
  String summary();

  /**
   * A successful migration result.
   *
   * @param configs The migrated configurations.
   * @param report The migration report.
   * @param summary The migration report summary.
   */
  record Success(ImmutableList<NamedConfig<?>> configs, String report, String summary)
      implements MigrationResult {}

  /**
   * A failed migration result.
   *
   * @param report The migration report.
   * @param summary The migration report summary.
   */
  record Failure(String report, String summary) implements MigrationResult {}
}
