package com.floragunn.searchguard.sgctl.config.migrator;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MigrationReporterImplTest {

  @Test
  public void testGenerateFullReport() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var value1 = Traceable.of(new Source.Attribute(cfgSrc, "a.b.c"), "value1");
    reporter.inconvertible(value1, "First message for value 1");
    reporter.inconvertible(value1, "Second message for value 1");
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.d"), 42), "Other config option");
    reporter.problem(value1, "Your message here");
    reporter.generic("Generic message 1");
    reporter.generic("Generic message 2");

    var expected =
        """
        # sgctl migrate-security report

        2 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
        * test.yml: a.b.c: value1
          * First message for value 1
          * Second message for value 1
        * test.yml: a.b.d: 42
          * Other config option

        1 setting(s) caused a generic problem:
        * test.yml: a.b.c: value1
          * Your message here

        2 other problem(s):
        * Generic message 1
        * Generic message 2
        """;

    assertEquals(expected, reporter.generateReport());
  }

  @Test
  public void testInconvertibleOmittedIfEmpty() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    reporter.problem(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.c"), "value1"), "Your message here");
    reporter.generic("Generic message");

    var expected =
        """
            # sgctl migrate-security report

            1 setting(s) caused a generic problem:
            * test.yml: a.b.c: value1
              * Your message here

            1 other problem(s):
            * Generic message
            """;

    assertEquals(expected, reporter.generateReport());
  }

  @Test
  public void testProblemOmittedIfEmpty() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "a.b.d"), 42), "Other config option");
    reporter.generic("Generic message");

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
  }

  @Test
  public void testGenericOmittedIfEmpty() {
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

            1 setting(s) caused a generic problem:
            * test.yml: a.b.c: value1
              * Your message here
            """;

    assertEquals(expected, reporter.generateReport());
  }
}
