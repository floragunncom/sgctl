package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.BaseTraceable;

/** For generating the migration report. */
public interface MigrationReporter {

  /**
   * Reports a critical problem with a {@link BaseTraceable}. Critical problems will cause migration
   * to fail, and no config files will be written.
   *
   * @param subject The {@link BaseTraceable} that is the subject of this problem.
   * @param message An explanation of the problem.
   */
  void critical(BaseTraceable<?> subject, String message);

  /**
   * Reports a genic problem with a {@link BaseTraceable}.
   *
   * @param subject The {@link BaseTraceable} that is the subject of this problem.
   * @param message An explanation of the problem.
   */
  void problem(BaseTraceable<?> subject, String message);

  /**
   * Reports a {@link BaseTraceable} as inconvertible, meaning that an equivalent concept does not
   * exist in the target domain and as such cannot be converted.
   *
   * @param subject The {@link BaseTraceable} that is inconvertible.
   * @param message Additional information, e.g. the action that was taken to resolve this problem
   */
  void inconvertible(BaseTraceable<?> subject, String message);

  /**
   * Adds a critical message to the report. The migration cannot complete successfully if any
   * critical messages are present.
   *
   * @param message The message.
   */
  void critical(String message);

  /**
   * Adds an uncategorized message to the report.
   *
   * @param message The message.
   */
  void problem(String message);

  /**
   * Reports that a default value was applied for an optional/missing field. This is informational
   * only and does not cause migration to fail.
   *
   * @param subject The {@link BaseTraceable} that is the subject of this default application.
   * @param fieldName The name of the field that received a default value.
   * @param defaultValue The default value that was applied.
   */
  void defaultApplied(BaseTraceable<?> subject, String fieldName, String defaultValue);

  /**
   * Generates the migration report from all logged problems.
   *
   * @return The report
   */
  String generateReport();

  /**
   * Generates a small summary of the migration report from all logged problems.
   *
   * @return The report summary
   */
  String generateReportSummary();

  /**
   * Returns whether any critical problems were reported. Critical problems will cause migration to
   * fail and no config files will be written.
   *
   * @return True if any critical problems were reported, false otherwise.
   */
  boolean hasCriticalProblems();

  /**
   * Creates a new migration reporter.
   *
   * @param title The title of the migration report
   * @param targetDomainName The name of the target domain, e.g. "Search Guard"
   * @return The migration reporter
   */
  static MigrationReporter of(String title, String targetDomainName) {
    return new MigrationReporterImpl(title, targetDomainName);
  }

  /**
   * Creates a new migration reporter for Search Guard migrations.
   *
   * @return The migration reporter
   */
  static MigrationReporter searchGuard() {
    return of("sgctl migrate-security report", "Search Guard");
  }
}
