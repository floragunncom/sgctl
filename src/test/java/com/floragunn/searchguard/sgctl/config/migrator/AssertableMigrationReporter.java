package com.floragunn.searchguard.sgctl.config.migrator;

import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.trace.BaseTraceable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssertableMigrationReporter implements MigrationReporter {

  private final MigrationReporter delegate =
      MigrationReporter.of("sgctl migrate-security report", "Search Guard");

  private final Map<BaseTraceable<?>, List<String>> critical = new LinkedHashMap<>();
  private final Map<BaseTraceable<?>, List<String>> problem = new LinkedHashMap<>();
  private final Map<BaseTraceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
  private final List<String> problemMessages = new ArrayList<>();
  private final List<String> criticalMessages = new ArrayList<>();

  @Override
  public void critical(BaseTraceable<?> subject, String message) {
    delegate.critical(subject, message);
    add(critical, subject, message);
  }

  @Override
  public void problem(BaseTraceable<?> subject, String message) {
    delegate.problem(subject, message);
    add(problem, subject, message);
  }

  @Override
  public void inconvertible(BaseTraceable<?> subject, String message) {
    delegate.inconvertible(subject, message);
    add(inconvertible, subject, message);
  }

  @Override
  public void problem(String message) {
    delegate.problem(message);
    problemMessages.add(message);
  }

  @Override
  public void critical(String message) {
    delegate.critical(message);
    criticalMessages.add(message);
  }

  @Override
  public String generateReport() {
    return delegate.generateReport();
  }

  @Override
  public String generateReportSummary() {
    return delegate.generateReportSummary();
  }

  @Override
  public boolean hasCriticalProblems() {
    return delegate.hasCriticalProblems();
  }

  private void add(
      Map<? super BaseTraceable<?>, List<String>> map, BaseTraceable<?> subject, String message) {
    map.computeIfAbsent(subject, v -> new ArrayList<>()).add(message);
  }

  /**
   * Asserts that a critical problem was reported for the given traceable. Then removes it from the
   * list of tracked critical problems.
   *
   * @param subject The traceable to check.
   */
  public void assertCritical(BaseTraceable<?> subject) {
    if (critical.remove(subject) == null)
      throw new AssertionError("Expected critical problem for traceable: " + subject);
  }

  /**
   * Asserts that there is no critical problem reported for the given traceable.
   *
   * @param subject The traceable to check.
   */
  public void assertNoCritical(BaseTraceable<?> subject) {
    if (critical.containsKey(subject))
      throw new AssertionError("Did not expect critical problem for traceable: " + subject);
  }

  /**
   * Asserts that a critical problem with the given message was reported for the given traceable.
   * Then removes the subject message combination from the list of tracked critical problems.
   *
   * @param subject The traceable to check.
   * @param message The message to check.
   */
  public void assertCritical(BaseTraceable<?> subject, String message) {
    List<String> messages = critical.get(subject);
    if (messages == null || !messages.remove(message))
      throw new AssertionError(
          "Expected critical message '" + message + "' for traceable: " + subject);
    if (messages.isEmpty()) {
      critical.remove(subject);
    }
  }

  /**
   * Asserts that a problem was reported for the given traceable. Then removes it from the list of
   * tracked problems.
   *
   * @param subject The traceable to check.
   */
  public void assertProblem(BaseTraceable<?> subject) {
    if (problem.remove(subject) == null)
      throw new AssertionError("Expected problem for traceable: " + subject);
  }

  /**
   * Asserts that there is no problem reported for the given traceable.
   *
   * @param subject The traceable to check.
   */
  public void assertNoProblem(BaseTraceable<?> subject) {
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
  public void assertProblem(BaseTraceable<?> subject, String message) {
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
  public void assertInconvertible(BaseTraceable<?> subject) {
    if (inconvertible.remove(subject) == null)
      throw new AssertionError("Expected inconvertible for traceable: " + subject);
  }

  /**
   * Asserts that there is no inconvertible reported for the given traceable.
   *
   * @param subject The traceable to check.
   */
  public void assertNoInconvertible(BaseTraceable<?> subject) {
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
  public void assertInconvertible(BaseTraceable<?> subject, String message) {
    List<String> messages = inconvertible.get(subject);
    if (messages == null || !messages.remove(message))
      throw new AssertionError(
          "Expected inconvertible message '" + message + "' for traceable: " + subject);
    if (messages.isEmpty()) {
      inconvertible.remove(subject);
    }
  }

  /**
   * Asserts that a critical message was reported. Then removes it from the list of tracked critical
   * problems.
   *
   * @param message The message to check.
   */
  public void assertCritical(String message) {
    if (!criticalMessages.remove(message))
      throw new AssertionError("Expected critical message: " + message);
  }

  /**
   * Asserts that a generic message was reported. Then removes it from the list of tracked generic
   * problems.
   *
   * @param message The message to check.
   */
  public void assertProblem(String message) {
    if (!problemMessages.remove(message))
      throw new AssertionError("Expected generic message: " + message);
  }

  /**
   * Asserts that a problem with the given message was reported for a traceable at the given path.
   * Then removes the message from the list of tracked problems.
   *
   * @param path The config path to search for (e.g. "elasticsearch.yml:
   *     xpack.security.authc.realms.ldap.ldap1.some_setting").
   * @param message The message to check.
   */
  public void assertProblem(String path, String message) {
    if (!removeMessageFromTraceableByPath(problem, path, message))
      throw new AssertionError("Expected problem message '" + message + "' for path: " + path);
  }

  /**
   * Asserts that a critical problem with the given message was reported for a traceable at the
   * given path. Then removes the message from the list of tracked critical problems.
   *
   * @param path The config path to search for.
   * @param message The message to check.
   */
  public void assertCritical(String path, String message) {
    if (!removeMessageFromTraceableByPath(critical, path, message))
      throw new AssertionError("Expected critical message '" + message + "' for path: " + path);
  }

  /**
   * Asserts that an inconvertible with the given message was reported for a traceable at the given
   * path. Then removes the message from the list of tracked inconvertibles.
   *
   * @param path The config path to search for.
   * @param message The message to check.
   */
  public void assertInconvertible(String path, String message) {
    if (!removeMessageFromTraceableByPath(inconvertible, path, message))
      throw new AssertionError(
          "Expected inconvertible message '" + message + "' for path: " + path);
  }

  private boolean removeMessageFromTraceableByPath(
      Map<BaseTraceable<?>, List<String>> map, String path, String message) {
    for (var entry : map.entrySet()) {
      if (entry.getKey().getSource().fullPathString().equals(path)) {
        if (entry.getValue().remove(message)) {
          if (entry.getValue().isEmpty()) {
            map.remove(entry.getKey());
          }
          return true;
        }
      }
    }
    return false;
  }

  /** Asserts that no more problems that weren't already checked via asserts were reported. */
  public void assertNoMoreProblems() {
    if (!problem.isEmpty()) throw new AssertionError("Unexpected problems: " + problem);
    if (!inconvertible.isEmpty())
      throw new AssertionError("Unexpected inconvertibles: " + inconvertible);
    if (!problemMessages.isEmpty())
      throw new AssertionError("Unexpected generic messages: " + problemMessages);
  }
}
