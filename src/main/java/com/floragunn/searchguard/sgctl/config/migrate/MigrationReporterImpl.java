package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.trace.BaseTraceable;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For generating the migration report. */
class MigrationReporterImpl implements MigrationReporter {
  private static final String HEADER_LINE =
          "----------------------------- %s -----------------------------";

  private static final String CRITICAL_TITLE =
          "  @|bold,red CRITICAL PROBLEMS (%d)|@\n"
                  + "  Settings that caused critical problems. Migration cannot complete while these exist.\n";
  private static final String INCONVERTIBLE_TITLE =
          "  @|bold,yellow INCONVERTIBLE (%d)|@\n"
                  + "  Settings that cannot be converted because no equivalent concept exists in %s.\n";
  private static final String PROBLEM_TITLE =
          "  @|bold,yellow PROBLEMS (%d)|@\n"
                  + "  Settings that caused other problems. Review them to ensure the migrated configuration behaves as expected.\n";
  private static final String OTHER_CRITICAL_TITLE =
          "  @|bold,red OTHER CRITICAL PROBLEMS (%d)|@\n"
                  + "  Critical problems not associated with a specific setting.\n";
  private static final String OTHER_PROBLEM_TITLE =
          "  @|bold,yellow OTHER PROBLEMS (%d)|@\n"
                  + "  Problems not associated with a specific setting.\n";

  private static final String KEY_TEMPLATE = "    - @|bold %s|@\n";
  private static final String SUB_MESSAGE_TEMPLATE = "        -> %s\n";
  private static final String FREE_MESSAGE_TEMPLATE = "    - %s\n";

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
    sb.append(format("@|bold " + HEADER_LINE + "|@", migrationTitle)).append("\n\n");

    Map<String, FileBuckets> byFile = new LinkedHashMap<>();
    collect(byFile, critical, Bucket.CRITICAL);
    collect(byFile, inconvertible, Bucket.INCONVERTIBLE);
    collect(byFile, problem, Bucket.PROBLEM);

    for (var entry : byFile.entrySet()) {
      sb.append(format("@|bold File - %s:|@", entry.getKey())).append("\n\n");
      FileBuckets buckets = entry.getValue();

      appendTraceableSection(sb, buckets.critical, CRITICAL_TITLE);
      appendTraceableSection(sb, buckets.inconvertible, INCONVERTIBLE_TITLE, targetDomainName);
      appendTraceableSection(sb, buckets.problem, PROBLEM_TITLE);
    }

    if (!criticalMessages.isEmpty() || !problemMessages.isEmpty()) {
      sb.append(format("@|bold General:|@")).append("\n\n");
      appendMessageSection(sb, criticalMessages, OTHER_CRITICAL_TITLE);
      appendMessageSection(sb, problemMessages, OTHER_PROBLEM_TITLE);
    }

    sb.append(format("@|bold " + HEADER_LINE + "|@", "End " + migrationTitle)).append("\n");
    return sb.toString();
  }

  @Override
  public String generateReportSummary() {
    var rootSb = new StringBuilder();
    rootSb.append("\n─── Migration Report Summary\n");

    StringBuilder sb = new StringBuilder();
    summarizeCount(sb, critical.size() + criticalMessages.size(), "critical problem(s) exist");
    summarizeCount(
        sb,
        inconvertible.size(),
        "setting(s) cannot be converted (no equivalent in " + targetDomainName + ")");
    summarizeCount(sb, problem.size() + problemMessages.size(), "other problem" + (problemMessages.size() > 1 ? "s" : "") + " exist");

    String issuesSummary = sb.toString();
    if (issuesSummary.isEmpty()) {
      rootSb.append("✓ No issues found\n");
    } else {
      rootSb.append(issuesSummary);
    }

    return rootSb.toString();
  }

  private void collect(
          Map<String, FileBuckets> byFile,
          Map<BaseTraceable<?>, List<String>> source,
          Bucket bucket) {
    for (var e : source.entrySet()) {
      String path = fileOf(e.getKey().getSource());
      FileBuckets buckets = byFile.computeIfAbsent(path, k -> new FileBuckets());
      buckets.get(bucket).put(e.getKey(), e.getValue());
    }
  }

  private void appendTraceableSection(
          StringBuilder sb,
          Map<BaseTraceable<?>, List<String>> traceables,
          String title,
          Object... titleArgs) {
    if (traceables.isEmpty()) return;

    Object[] args = new Object[titleArgs.length + 1];
    args[0] = traceables.size();
    System.arraycopy(titleArgs, 0, args, 1, titleArgs.length);
    sb.append(format(title, args));

    for (var entry : traceables.entrySet()) {
      var traceable = entry.getKey();
      var messages = entry.getValue();
      sb.append(format(KEY_TEMPLATE, keyLabel(traceable)));
      for (var message : messages) {
        sb.append(String.format(SUB_MESSAGE_TEMPLATE, message));
      }
    }
    sb.append("\n");
  }

  private void appendMessageSection(StringBuilder sb, List<String> messages, String title) {
    if (messages.isEmpty()) return;
    sb.append(format(title, messages.size()));
    for (var message : messages) {
      sb.append(String.format(FREE_MESSAGE_TEMPLATE, message));
    }
    sb.append("\n");
  }

  private void summarizeCount(StringBuilder sb, int count, String desc) {
    if (count == 0) return;

    sb.append("  • ");
    sb.append(count);
    sb.append(" ");
    sb.append(desc);
    sb.append("\n");
  }

  private static String format(String pattern, Object... args) {
    return CommandLine.Help.Ansi.AUTO.string(String.format(pattern, args));
  }

  private static String keyLabel(BaseTraceable<?> traceable) {
    var source = traceable.getSource();
    String innerPath = innerPathOf(source);
    if (innerPath.isBlank()) {
      return String.valueOf(traceable);
    }
    return innerPath;
  }

  private static String fileOf(Source source) {
    for (var part : source.path()) {
      if (part instanceof Source.Config cfg) {
        return cfg.file();
      }
    }
    return "General";
  }

  private static String innerPathOf(Source source) {
    var path = source.path();
    int start = 0;
    if (!path.isEmpty() && path.get(0) instanceof Source.Config) {
      start = 1;
    }
    return path.subList(start, path.size()).stream()
            .map(Source::pathPart)
            .filter(p -> !p.isEmpty())
            .reduce((a, b) -> a + "." + b)
            .orElse("");
  }

  private enum Bucket {
    CRITICAL,
    INCONVERTIBLE,
    PROBLEM
  }

  private static final class FileBuckets {
    final Map<BaseTraceable<?>, List<String>> critical = new LinkedHashMap<>();
    final Map<BaseTraceable<?>, List<String>> inconvertible = new LinkedHashMap<>();
    final Map<BaseTraceable<?>, List<String>> problem = new LinkedHashMap<>();

    Map<BaseTraceable<?>, List<String>> get(Bucket b) {
      return switch (b) {
        case CRITICAL -> critical;
        case INCONVERTIBLE -> inconvertible;
        case PROBLEM -> problem;
      };
    }
  }
}
