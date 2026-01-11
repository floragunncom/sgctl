package com.floragunn.searchguard.sgctl.config.migrator;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import org.junit.jupiter.api.Test;

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

  @Test
  public void testCriticalSecretRemembered() {
    var reporter = MigrationReporter.searchGuard();
    reporter.problem("Non-critical problem");
    assertFalse(reporter.hasCriticalProblems());

    var cfgSrc = new Source.Config("test.yml");
    var secret = Traceable.of(new Source.Attribute(cfgSrc, "password"), "supersecret");
    reporter.criticalSecret(secret, "Password is too weak");
    assertTrue(reporter.hasCriticalProblems());
  }

  @Test
  public void testSecretValuesCensored() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var secret = Traceable.of(new Source.Attribute(cfgSrc, "password"), "supersecret");
    reporter.criticalSecret(secret, "Password is too weak");
    reporter.problemSecret(
        Traceable.of(new Source.Attribute(cfgSrc, "api_key"), "secret-api-key-123"),
        "API key format is deprecated");
    reporter.inconvertibleSecret(
        Traceable.of(new Source.Attribute(cfgSrc, "token"), "jwt-token-value"),
        "Token format not supported");

    var expected =
        """
        # sgctl migrate-security report

        1 setting(s) caused critical problem(s):
        * test.yml: password: ***
          * Password is too weak

        1 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
        * test.yml: token: ***
          * Token format not supported

        1 setting(s) caused other problem(s):
        * test.yml: api_key: ***
          * API key format is deprecated
        """;

    assertEquals(expected, reporter.generateReport());
  }

  @Test
  public void testSecretAndNonSecretMerged() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var normalValue = Traceable.of(new Source.Attribute(cfgSrc, "username"), "admin");
    var secretValue = Traceable.of(new Source.Attribute(cfgSrc, "password"), "supersecret");

    reporter.critical(normalValue, "Username is reserved");
    reporter.criticalSecret(secretValue, "Password is too weak");

    var expected =
        """
        # sgctl migrate-security report

        2 setting(s) caused critical problem(s):
        * test.yml: username: admin
          * Username is reserved
        * test.yml: password: ***
          * Password is too weak
        """;

    assertEquals(expected, reporter.generateReport());
    assertTrue(reporter.hasCriticalProblems());
  }

  @Test
  public void testMixedSecretsAndNonSecretsAllCategories() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");

    // Critical: one normal, one secret
    reporter.critical(
        Traceable.of(new Source.Attribute(cfgSrc, "setting1"), "value1"), "Normal critical");
    reporter.criticalSecret(
        Traceable.of(new Source.Attribute(cfgSrc, "secret1"), "secret-value1"), "Secret critical");

    // Inconvertible: one normal, one secret
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "setting2"), "value2"), "Normal inconvertible");
    reporter.inconvertibleSecret(
        Traceable.of(new Source.Attribute(cfgSrc, "secret2"), "secret-value2"),
        "Secret inconvertible");

    // Problem: one normal, one secret
    reporter.problem(
        Traceable.of(new Source.Attribute(cfgSrc, "setting3"), "value3"), "Normal problem");
    reporter.problemSecret(
        Traceable.of(new Source.Attribute(cfgSrc, "secret3"), "secret-value3"), "Secret problem");

    var expected =
        """
        # sgctl migrate-security report

        2 setting(s) caused critical problem(s):
        * test.yml: setting1: value1
          * Normal critical
        * test.yml: secret1: ***
          * Secret critical

        2 setting(s) cannot be converted because no equivalent concept exists in Search Guard:
        * test.yml: setting2: value2
          * Normal inconvertible
        * test.yml: secret2: ***
          * Secret inconvertible

        2 setting(s) caused other problem(s):
        * test.yml: setting3: value3
          * Normal problem
        * test.yml: secret3: ***
          * Secret problem
        """;

    assertEquals(expected, reporter.generateReport());
  }
}
