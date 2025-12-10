package com.floragunn.searchguard.sgctl.config.migrator;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MigrationReporterTest {

  @Test
  public void testGenerateFullReport() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var value1 = Traceable.of(new Source.Attribute(cfgSrc, "a.b.c"), "value1");
    reporter.critical(value1, "Critical setting");
    reporter.critical("Critical generic");
    reporter.inconvertible(value1, "First message for value 1");
    reporter.inconvertible(value1, "Second message for value 1");
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.d"), 42), "Other config option");
    reporter.problem(value1, "Your message here");
    reporter.problem("Generic message 1");
    reporter.problem("Generic message 2");

    var expected =
        """
        # sgctl migrate-security report

        1 setting(s) caused critical problem(s):
        * test.yml: a.b.c: value1
          * Critical setting

        1 other critical problem(s):
        * Critical generic

        2 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
        * test.yml: a.b.c: value1
          * First message for value 1
          * Second message for value 1
        * test.yml: a.b.d: 42
          * Other config option

        1 setting(s) caused other problem(s):
        * test.yml: a.b.c: value1
          * Your message here

        2 other problem(s):
        * Generic message 1
        * Generic message 2
        """;

    assertEquals(expected, reporter.generateReport());
    assertTrue(reporter.hasCriticalProblems());
  }

  @Test
  public void testInconvertibleOmittedIfEmpty() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    reporter.problem(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.c"), "value1"), "Your message here");
    reporter.problem("Generic message");

    var expected =
        """
            # sgctl migrate-security report

            1 setting(s) caused other problem(s):
            * test.yml: a.b.c: value1
              * Your message here

            1 other problem(s):
            * Generic message
            """;

    assertEquals(expected, reporter.generateReport());
    assertFalse(reporter.hasCriticalProblems());
  }

  @Test
  public void testProblemOmittedIfEmpty() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.d"), 42), "Other config option");
    reporter.problem("Generic message");

    var expected =
        """
            # sgctl migrate-security report

            1 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
            * test.yml: a.b.d: 42
              * Other config option

            1 other problem(s):
            * Generic message
            """;

    assertEquals(expected, reporter.generateReport());
    assertFalse(reporter.hasCriticalProblems());
  }

  @Test
  public void testProblemMessageOmittedIfEmpty() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var value1 = Traceable.of(new Source.Attribute(cfgSrc, "a.b.c"), "value1");
    reporter.inconvertible(value1, "First message for value 1");
    reporter.problem(value1, "Your message here");

    var expected =
        """
            # sgctl migrate-security report

            1 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
            * test.yml: a.b.c: value1
              * First message for value 1

            1 setting(s) caused other problem(s):
            * test.yml: a.b.c: value1
              * Your message here
            """;

    assertEquals(expected, reporter.generateReport());
    assertFalse(reporter.hasCriticalProblems());
  }

  @Test
  public void testCriticalRemembered() {
    var reporter = MigrationReporter.searchGuard();
    reporter.problem("Non-critical problem");
    assertFalse(reporter.hasCriticalProblems());

    var reporter1 = MigrationReporter.searchGuard();
    reporter1.critical("There was a critical problem");
    assertTrue(reporter1.hasCriticalProblems());
  }
}
