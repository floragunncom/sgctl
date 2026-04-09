package com.floragunn.searchguard.sgctl.config.migrator;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MigrationReporterTest {

  @BeforeAll
  public static void disableAnsi() {
    // Make picocli's Ansi.AUTO deterministic in tests.
    System.setProperty("picocli.ansi", "false");
  }

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
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              CRITICAL PROBLEMS (1)
              Settings that caused critical problems. Migration cannot complete while these exist.
                - a.b.c
                    -> Critical setting
    
              INCONVERTIBLE (2)
              Settings that cannot be converted because no equivalent concept exists in Search Guard.
                - a.b.c
                    -> First message for value 1
                    -> Second message for value 1
                - a.b.d
                    -> Other config option
    
              PROBLEMS (1)
              Settings that caused other problems. Review them to ensure the migrated configuration behaves as expected.
                - a.b.c
                    -> Your message here
    
            General:
    
              OTHER CRITICAL PROBLEMS (1)
              Critical problems not associated with a specific setting.
                - Critical generic
    
              OTHER PROBLEMS (2)
              Problems not associated with a specific setting.
                - Generic message 1
                - Generic message 2
    
            ----------------------------- End sgctl migrate-security report -----------------------------
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
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              PROBLEMS (1)
              Settings that caused other problems. Review them to ensure the migrated configuration behaves as expected.
                - a.b.c
                    -> Your message here
    
            General:
    
              OTHER PROBLEMS (1)
              Problems not associated with a specific setting.
                - Generic message
    
            ----------------------------- End sgctl migrate-security report -----------------------------
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
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              INCONVERTIBLE (1)
              Settings that cannot be converted because no equivalent concept exists in Search Guard.
                - a.b.d
                    -> Other config option
    
            General:
    
              OTHER PROBLEMS (1)
              Problems not associated with a specific setting.
                - Generic message
    
            ----------------------------- End sgctl migrate-security report -----------------------------
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
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              INCONVERTIBLE (1)
              Settings that cannot be converted because no equivalent concept exists in Search Guard.
                - a.b.c
                    -> First message for value 1
    
              PROBLEMS (1)
              Settings that caused other problems. Review them to ensure the migrated configuration behaves as expected.
                - a.b.c
                    -> Your message here
    
            ----------------------------- End sgctl migrate-security report -----------------------------
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
  public void testSecretValuesCensored() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var secret = Traceable.of(new Source.Attribute(cfgSrc, "password"), "supersecret", true);
    reporter.critical(secret, "Password is too weak");

    String report = reporter.generateReport();

    assertTrue(report.contains("File - test.yml:"), report);
    assertTrue(report.contains("- password"), report);
    assertTrue(report.contains("-> Password is too weak"), report);
    assertFalse(report.contains("supersecret"), report);
  }

  @Test
  public void testSecretAndNonSecretMerged() {
    var reporter = MigrationReporter.searchGuard();
    var cfgSrc = new Source.Config("test.yml");
    var normalValue = Traceable.of(new Source.Attribute(cfgSrc, "username"), "admin");
    var secretValue = Traceable.of(new Source.Attribute(cfgSrc, "password"), "supersecret", true);

    reporter.critical(normalValue, "Username is reserved");
    reporter.critical(secretValue, "Password is too weak");

    var expected =
            """
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              CRITICAL PROBLEMS (2)
              Settings that caused critical problems. Migration cannot complete while these exist.
                - username
                    -> Username is reserved
                - password
                    -> Password is too weak
    
            ----------------------------- End sgctl migrate-security report -----------------------------
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
    reporter.critical(
        Traceable.of(new Source.Attribute(cfgSrc, "secret1"), "secret-value1", true),
        "Secret critical");

    // Inconvertible: one normal, one secret
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "setting2"), "value2"), "Normal inconvertible");
    reporter.inconvertible(
        Traceable.of(new Source.Attribute(cfgSrc, "secret2"), "secret-value2", true),
        "Secret inconvertible");

    // Problem: one normal, one secret
    reporter.problem(
        Traceable.of(new Source.Attribute(cfgSrc, "setting3"), "value3"), "Normal problem");
    reporter.problem(
        Traceable.of(new Source.Attribute(cfgSrc, "secret3"), "secret-value3", true),
        "Secret problem");

    var expected =
            """
            ----------------------------- sgctl migrate-security report -----------------------------
    
            File - test.yml:
    
              CRITICAL PROBLEMS (2)
              Settings that caused critical problems. Migration cannot complete while these exist.
                - setting1
                    -> Normal critical
                - secret1
                    -> Secret critical
    
              INCONVERTIBLE (2)
              Settings that cannot be converted because no equivalent concept exists in Search Guard.
                - setting2
                    -> Normal inconvertible
                - secret2
                    -> Secret inconvertible
    
              PROBLEMS (2)
              Settings that caused other problems. Review them to ensure the migrated configuration behaves as expected.
                - setting3
                    -> Normal problem
                - secret3
                    -> Secret problem
    
            ----------------------------- End sgctl migrate-security report -----------------------------
            """;

    assertEquals(expected, reporter.generateReport());
  }
}
