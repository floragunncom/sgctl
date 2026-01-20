package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.*;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNodeParser;
import com.floragunn.searchguard.sgctl.config.xpack.*;
import com.floragunn.searchguard.sgctl.util.StringUtils;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "migrate-security",
    description = "Converts X-Pack configs to Search Guard configs")
public class XPackMigrate implements Callable<Integer> {

  public static final String HELP_MESSAGE =
      """
      This migration tool expects the following files in the input directory (--input-dir):
      - role_mappings.json, from the .security index via GET /_security/role_mapping
      - roles.json, from the .security index via GET /_security/role
      - users.json, from the .security index via GET /_security/user
      - elasticsearch.yml, from the Elasticsearch config directory
      - kibana.yml, from the Kibana config directory
      The tool will automatically convert them and write the results to the output directory (--output-dir), alongside a migration report (report.md).
      """;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Show help screen and exit")
  boolean help;

  @CommandLine.Option(
      names = {"-i", "--input-dir"},
      description = "Location of the old x-pack configuration files",
      required = true)
  Path inputDir;

  @CommandLine.Option(
      names = {"-o", "--output-dir"},
      description = "Directory where to write new sg configuration files",
      required = true)
  Path outputDir;

  @CommandLine.Option(
      names = {"--overwrite"},
      description = "Whether existing output configuration files should be overwritten",
      defaultValue = "false")
  boolean overwrite;

  private static final Map<String, TraceableDocNodeParser<Object>> configParsers =
      Map.of(
          "role_mappings.json", RoleMappings::parse,
          "roles.json", Roles::parse,
          "users.json", Users::parse,
          "elasticsearch.yml", XPackElasticsearchConfig::parse,
          "kibana.yml", Kibana::parse);

  public Integer call() throws Exception {
    registerSubMigrators();

    if (!Files.isDirectory(inputDir)) {
      System.err.println("Error: Input is not a directory: " + inputDir.toAbsolutePath());
      return 1;
    }
    System.out.println("Welcome to the Search Guard X-pack security migration tool.\n\n");
    try {
      // deserialize
      final var xPackConfigs = parseConfigs();
      if (xPackConfigs.isEmpty()) {
        System.err.println(
            "Error: No X-Pack configuration files found in input directory: "
                + inputDir.toAbsolutePath());
        return 1;
      }

      // migrate
      final Migrator migrator = new Migrator();
      final Migrator.MigrationContext context = getMigrationContext(xPackConfigs);
      final MigrationResult result = migrator.migrate(context);
      // serialize
      if (result instanceof MigrationResult.Success suc) {
        checkOverwrite(suc.configs());
        writeConfigs(suc.configs());
      } else {
        checkOverwrite(List.of());
      }
      // report
      System.out.println(result.summary());
      System.out.println("NOTE: The full report is saved to: " + outputDir.resolve("report.md"));
      writeReport(result.report());
    } catch (SgctlException e) {
      System.err.println("Error: " + e.getMessage());
      return 1;
    }
    return 0;
  }

  private Migrator.MigrationContext getMigrationContext(Map<String, Object> xPackConfigs) {
    return new Migrator.MigrationContext(
        Optional.ofNullable((RoleMappings) xPackConfigs.get("role_mappings.json")),
        Optional.ofNullable((Roles) xPackConfigs.get("roles.json")),
        Optional.ofNullable((Users) xPackConfigs.get("users.json")),
        Optional.ofNullable((XPackElasticsearchConfig) xPackConfigs.get("elasticsearch.yml")),
        Optional.ofNullable((Kibana) xPackConfigs.get("kibana.yml")));
  }

  private void registerSubMigrators() {
    var registry = MigratorRegistry.getInstance();
    registry.registerSubMigrator(new RolesMigrator());
    registry.registerSubMigrator(new AuthMigrator());
    registry.registerSubMigrator(new UserMigrator());
    registry.registerSubMigrator(new RoleMappingsMigrator());
    registry.registerSubMigrator(new FrontendAuthMigrator());
    registry.finalizeSubMigrators(); // Never forget
  }

  private Map<String, Object> parseConfigs() throws SgctlException, IOException {
    final Map<String, Object> configs = new HashMap<>();
    final Map<String, ValidationErrors> errors = new HashMap<>();

    for (final var entry : configParsers.entrySet()) {
      final var configFileName = entry.getKey();
      final var parserFunction = entry.getValue();
      // Ensure valid path
      final var configPath = inputDir.resolve(configFileName);
      if (!Files.isReadable(configPath)) {
        continue;
      }
      if (Files.isDirectory(configPath)) {
        throw new SgctlException("Config is a directory, but should be a file: " + configFileName);
      }
      try {
        // Read to Object
        final DocNode config;
        if (configFileName.endsWith(".yaml") || configFileName.endsWith(".yml")) {
          config = DocNode.wrap(DocReader.yaml().read(configPath.toFile()));
        } else if (configFileName.endsWith(".json")) {
          config = DocNode.wrap(DocReader.json().read(configPath.toFile()));
        } else {
          throw new SgctlException("Invalid config file extension: " + configFileName);
        }

        // Parse config
        var parsed =
            TraceableDocNode.parse(config, new Source.Config(configFileName), parserFunction);
        configs.put(configFileName, parsed);
      } catch (ConfigValidationException e) {
        // accumulate all errors and report them together below
        errors.put(configFileName, e.getValidationErrors());
      }
    }

    if (!errors.isEmpty()) {
      var prettyError =
          errors.entrySet().stream()
              .map(
                  e -> {
                    var prettySingleError = StringUtils.indentLines(e.getValue().toString(), 2);
                    return "\t" + e.getKey() + ":\n" + prettySingleError;
                  })
              .collect(Collectors.joining("\n"));
      throw new SgctlException("Failed to parse config(s) (migration aborted):\n" + prettyError);
    }

    return configs;
  }

  private void checkOverwrite(List<NamedConfig<?>> configs) throws SgctlException {
    final var additionalFileNames = List.of("report.md");
    final var configFileNames = configs.stream().map(NamedConfig::getFileName);
    final var fileNames = Streams.concat(configFileNames, additionalFileNames.stream());
    final var wouldOverwrite =
        fileNames
            .filter(Objects::nonNull)
            .filter(fileName -> Files.exists(outputDir.resolve(fileName)))
            .toList();
    if (!overwrite && !wouldOverwrite.isEmpty()) {
      final String overwriteFileNames = String.join(", ", wouldOverwrite);
      final String fileWord = (wouldOverwrite.size() == 1) ? "file" : "files";
      throw new SgctlException(
          "Refusing to output: Would overwrite existing " + fileWord + ": " + overwriteFileNames);
    }
  }

  private void writeConfigs(List<NamedConfig<?>> configs) throws IOException, SgctlException {
    if (!Files.exists(outputDir)) {
      Files.createDirectory(outputDir);
    }

    for (final var config : configs) {
      final Object configObj = config.toBasicObject();
      final Path configPath = outputDir.resolve(config.getFileName());

      DocWriter.yaml().write(configPath.toFile(), configObj);
    }
  }

  private void writeReport(String report) throws IOException, SgctlException {
    if (!Files.exists(outputDir)) {
      Files.createDirectories(outputDir);
    }

    var reportFile = outputDir.resolve("report.md");
    Files.writeString(reportFile, report);
  }
}
