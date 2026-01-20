package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.BaseTraceable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For generating the migration report. */
class MigrationReporterImpl implements MigrationReporter {

  private final String migrationTitle;
  private final String targetDomainName;

  private final Map<BaseTraceable<?>, List<String>> problem = new LinkedHashMap<>();
  private final Map<BaseTraceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
  private final Map<BaseTraceable<?>, List<String>> critical = new LinkedHashMap<>();
  private final List<String> problemMessages = new ArrayList<>();
  private final List<String> criticalMessages = new ArrayList<>();

  public MigrationReporterImpl(String migrationTitle, String targetDomainName) {
    this.migrationTitle = migrationTitle;
    this.targetDomainName = targetDomainName;
  }

  @Override
  public void critical(BaseTraceable<?> subject, String message) {
    add(critical, subject, message);
  }

  @Override
  public void problem(BaseTraceable<?> subject, String message) {
    add(problem, subject, message);
  }

  @Override
  public void inconvertible(BaseTraceable<?> subject, String message) {
    add(inconvertible, subject, message);
  }

  @Override
  public void problem(String message) {
    problemMessages.add(message);
  }

  @Override
  public void critical(String message) {
    criticalMessages.add(message);
  }

  @Override
  public boolean hasCriticalProblems() {
    return !criticalMessages.isEmpty() || !critical.isEmpty();
  }

  private void add(
      Map<? super BaseTraceable<?>, List<String>> map, BaseTraceable<?> subject, String message) {
    map.computeIfAbsent(subject, v -> new ArrayList<>()).add(message);
  }

  @Override
  public String generateReport() {
    var sb = new StringBuilder();
    sb.append("# ");
    sb.append(migrationTitle);
    sb.append("\n");

    reportTraceables(sb, critical, "setting(s) caused critical problem(s)");
    reportList(sb, criticalMessages, "other critical problem(s)");

    reportTraceables(
        sb,
        inconvertible,
        "setting(s) cannot be converted because no equivalent concept exists in "
            + targetDomainName);
    reportTraceables(sb, problem, "setting(s) caused other problem(s)");
    reportList(sb, problemMessages, "other problem(s)");

    return sb.toString();
  }

  @Override
  public String generateReportSummary() {
    var rootSb = new StringBuilder();
    rootSb.append(migrationTitle);
    rootSb.append(" summary:");

    StringBuilder sb = new StringBuilder();
    summarizeTraceables(sb, critical, "setting(s) caused critical problem(s)");
    summarizeList(sb, criticalMessages, "other critical problem(s) exist");
    summarizeTraceables(
        sb,
        inconvertible,
        "setting(s) cannot be converted because no equivalent concept exists in "
            + targetDomainName);
    summarizeTraceables(sb, problem, "setting(s) caused other problem(s)");
    summarizeList(sb, problemMessages, "other problem(s) exist");

    String issuesSummary = sb.toString();
    if (issuesSummary.isEmpty()) {
      rootSb.append("\n\tNo issues were found.");
    } else {
      rootSb.append(issuesSummary);
    }

    return rootSb.toString();
  }

  private void summarizeTraceables(
      StringBuilder sb,
      Map<? extends BaseTraceable<?>, ? extends List<String>> traceables,
      String desc) {
    if (traceables.isEmpty()) return;
    sb.append("\n\t* ");

    sb.append(traceables.size());
    sb.append(" ");
    sb.append(desc);
  }

  private void reportTraceables(
      StringBuilder sb,
      Map<? extends BaseTraceable<?>, ? extends List<String>> traceables,
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
      sb.append(traceable);
      sb.append("\n");

      for (var message : messages) {
        sb.append("  * ");
        sb.append(message);
        sb.append("\n");
      }
    }
  }

  private void summarizeList(StringBuilder sb, List<? extends String> messages, String desc) {
    if (messages.isEmpty()) return;

    sb.append("\n\t* ");

    sb.append(messages.size());
    sb.append(" ");
    sb.append(desc);
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
