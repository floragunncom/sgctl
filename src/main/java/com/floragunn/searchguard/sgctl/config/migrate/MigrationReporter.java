package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.Traceable;

/** For generating the migration report. */
public interface MigrationReporter {

  /**
   * Reports a genic problem with a {@link Traceable}.
   *
   * @param subject The {@link Traceable} that is the subject of this problem.
   * @param message An explanation of the problem.
   */
  void problem(Traceable<?> subject, String message);

  /**
   * Reports a {@link Traceable} as inconvertible, meaning that an equivalent concept does not exist
   * in the target domain and as such cannot be converted.
   *
   * @param subject The {@link Traceable} that is inconvertible.
   * @param message Additional information, e.g. the action that was taken to resolve this problem
   */
  void inconvertible(Traceable<?> subject, String message);

  /**
   * Adds an uncategorized message to the report.
   *
   * @param message The message.
   */
  void generic(String message);

  /**
   * Generates the migration report from all logged problems.
   *
   * @return The report
   */
  String generateReport();

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
