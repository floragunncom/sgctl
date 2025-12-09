package com.floragunn.searchguard.sgctl.config.migrator;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssertableMigrationReporter implements MigrationReporter {

  private final MigrationReporter delegate =
      MigrationReporter.of("sgctl migrate-security report", "Search Guard");

  private final Map<Traceable<?>, List<String>> problem = new LinkedHashMap<>();
  private final Map<Traceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
  private final List<String> generic = new ArrayList<>();

  @Override
  public void problem(Traceable<?> subject, String message) {
    delegate.problem(subject, message);
    add(problem, subject, message);
  }

  @Override
  public void inconvertible(Traceable<?> subject, String message) {
    delegate.inconvertible(subject, message);
    add(inconvertible, subject, message);
  }

  @Override
  public void generic(String message) {
    delegate.generic(message);
    generic.add(message);
  }

  @Override
  public String generateReport() {
    return delegate.generateReport();
  }

  private void add(
      Map<? super Traceable<?>, List<String>> map, Traceable<?> subject, String message) {
    map.computeIfAbsent(subject, v -> new ArrayList<>()).add(message);
  }

  /**
   * Asserts that a problem was reported for the given traceable. Then removes it from the list of
   * tracked problems.
   *
   * @param subject The traceable to check.
   */
  public void assertProblem(Traceable<?> subject) {
    if (problem.remove(subject) == null)
      throw new AssertionError("Expected problem for traceable: " + subject);
  }

  /**
   * Asserts that there is no problem reported for the given traceable.
   *
   * @param subject The traceable to check.
   */
  public void assertNoProblem(Traceable<?> subject) {
    if (problem.containsKey(subject))
      throw new AssertionError("Did not expect problem for traceable: " + subject);
  }

  /**
   * Asserts that a problem with the given message was reported for the given traceable. Then
   * removes the subject message combination from the list of tracked problems.
   *
   * @param subject The traceable to check.
   * @param message The message to check.
   */
  public void assertProblem(Traceable<?> subject, String message) {
    List<String> messages = problem.get(subject);
    if (messages == null || !messages.remove(message))
      throw new AssertionError(
          "Expected problem message '" + message + "' for traceable: " + subject);
    if (messages.isEmpty()) {
      problem.remove(subject);
    }
  }

  /**
   * Asserts that an inconvertible was reported for the given traceable. Then removes it from the
   * list of tracked inconvertibles.
   *
   * @param subject The traceable to check.
   */
  public void assertInconvertible(Traceable<?> subject) {
    if (inconvertible.remove(subject) == null)
      throw new AssertionError("Expected inconvertible for traceable: " + subject);
  }

  /**
   * Asserts that there is no inconvertible reported for the given traceable.
   *
   * @param subject The traceable to check.
   */
  public void assertNoInconvertible(Traceable<?> subject) {
    if (inconvertible.containsKey(subject))
      throw new AssertionError("Did not expect inconvertible for traceable: " + subject);
  }

  /**
   * Asserts that an inconvertible with the given message was reported for the given traceable. Then
   * removes the subject message combination from the list of tracked inconvertibles.
   *
   * @param subject The traceable to check.
   * @param message The message to check.
   */
  public void assertInconvertible(Traceable<?> subject, String message) {
    List<String> messages = inconvertible.get(subject);
    if (messages == null || !messages.remove(message))
      throw new AssertionError(
          "Expected inconvertible message '" + message + "' for traceable: " + subject);
    if (messages.isEmpty()) {
      inconvertible.remove(subject);
    }
  }

  /**
   * Asserts that a generic message was reported. Then removes it from the list of tracked generic
   *
   * @param message The message to check.
   */
  public void assertGeneric(String message) {
    if (!generic.remove(message)) throw new AssertionError("Expected generic message: " + message);
  }

  /** Asserts that no more problems that weren't already checked via asserts were reported. */
  public void assertNoMoreProblems() {
    if (!problem.isEmpty()) throw new AssertionError("Unexpected problems: " + problem);
    if (!inconvertible.isEmpty())
      throw new AssertionError("Unexpected inconvertibles: " + inconvertible);
    if (!generic.isEmpty()) throw new AssertionError("Unexpected generic messages: " + generic);
  }
}
