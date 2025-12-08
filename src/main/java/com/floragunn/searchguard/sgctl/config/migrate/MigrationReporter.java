package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For generating the migration report. */
public class MigrationReporter {

  private final Map<Traceable<?>, List<String>> problem = new LinkedHashMap<>();
  private final Map<Traceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
  private final List<String> generic = new ArrayList<>();

  /**
   * Reports a genic problem with a {@link Traceable}.
   *
   * @param subject The {@link Traceable} that is the subject of this problem.
   * @param message An explanation of the problem.
   */
  public void problem(Traceable<?> subject, String message) {
    add(problem, subject, message);
  }

  /**
   * Reports a {@link Traceable} as inconvertible, meaning that an equivalent concept does not exist
   * in the target domain and as such cannot be converted.
   *
   * @param subject The {@link Traceable} that is inconvertible.
   * @param message Additional information, e.g. the action that was taken to resolve this problem
   */
  public void inconvertible(Traceable<?> subject, String message) {
    add(inconvertible, subject, message);
  }

  /**
   * Adds an uncategorized message to the report.
   *
   * @param message The message.
   */
  public void generic(String message) {
    generic.add(message);
  }

  private void add(
      Map<? super Traceable<?>, List<String>> map, Traceable<?> subject, String message) {
    map.computeIfAbsent(subject, v -> new ArrayList<>()).add(message);
  }

  /**
   * Generates the migration report from all logged problems.
   *
   * @return The report
   */
  public String generateReport() {
    var sb = new StringBuilder();
    sb.append("# sgctl migrate-security report\n");

    reportTraceables(
        sb,
        inconvertible,
        "setting(s) cannot be converted because no equivalent concept exists in Search Guard");
    reportTraceables(sb, problem, "setting(s) caused a generic problem");
    reportList(sb, generic, "other problem(s)");

    return sb.toString();
  }

  private void reportTraceables(
      StringBuilder sb,
      Map<? extends Traceable<?>, ? extends List<String>> traceables,
      String desc) {
    if (traceables.isEmpty()) return;

    sb.append("\n");

    sb.append(traceables.size());
    sb.append(" ");
    sb.append(desc);
    sb.append(":\n");

    for (var entry : traceables.entrySet()) {
      var traceable = entry.getKey();
      var messages = entry.getValue();

      sb.append("* ");
      sb.append(entry.getKey().getSource().fullPathString());
      sb.append(": ");
      sb.append(traceable.get());
      sb.append("\n");

      for (var message : messages) {
        sb.append("  * ");
        sb.append(message);
        sb.append("\n");
      }
    }
  }

  private void reportList(StringBuilder sb, List<? extends String> messages, String desc) {
    if (messages.isEmpty()) return;

    sb.append("\n");

    sb.append(messages.size());
    sb.append(" ");
    sb.append(desc);
    sb.append(":\n");

    for (var message : messages) {
      sb.append("* ");
      sb.append(message);
      sb.append("\n");
    }
  }
}
