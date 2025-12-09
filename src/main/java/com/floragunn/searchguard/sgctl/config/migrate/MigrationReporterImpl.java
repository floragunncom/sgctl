package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For generating the migration report. */
class MigrationReporterImpl implements MigrationReporter {

  private final String migrationTitle;
  private final String targetDomainName;

  private final Map<Traceable<?>, List<String>> problem = new LinkedHashMap<>();
  private final Map<Traceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
  private final List<String> generic = new ArrayList<>();

  public MigrationReporterImpl(String migrationTitle, String targetDomainName) {
    this.migrationTitle = migrationTitle;
    this.targetDomainName = targetDomainName;
  }

  @Override
  public void problem(Traceable<?> subject, String message) {
    add(problem, subject, message);
  }

  @Override
  public void inconvertible(Traceable<?> subject, String message) {
    add(inconvertible, subject, message);
  }

  @Override
  public void generic(String message) {
    generic.add(message);
  }

  private void add(
      Map<? super Traceable<?>, List<String>> map, Traceable<?> subject, String message) {
    map.computeIfAbsent(subject, v -> new ArrayList<>()).add(message);
  }

  @Override
  public String generateReport() {
    var sb = new StringBuilder();
    sb.append("# ");
    sb.append(migrationTitle);
    sb.append("\n");

    reportTraceables(
        sb,
        inconvertible,
        "setting(s) cannot be converted because no equivalent concept exists in "
            + targetDomainName);
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
