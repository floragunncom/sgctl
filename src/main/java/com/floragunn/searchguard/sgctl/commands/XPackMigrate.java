package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.MigratorRegistry;
import com.floragunn.searchguard.sgctl.config.migrate.auth.AuthMigrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "migrate-security",
    description = "Converts X-Pack configs to Search Guard configs")
public class XPackMigrate implements Callable<Integer> {

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

  private static final Map<String, Parser<Object, Parser.Context>> configParsers =
      Map.of(
          // TODO: Add parsing functions here <filename>,Record::parse
          "role_mapping.json", RoleMappings::parse,
          "roles.json", Roles::parse,
          "elasticsearch.yml", XPackElasticsearchConfig::parse);

  public Integer call() throws Exception {
    registerSubMigrators();

    if (!Files.isDirectory(inputDir)) {
      System.err.println("Error: Input is not a directory: " + inputDir.toAbsolutePath());
      return 1;
    }
    System.out.println("Welcome to the Search Guard X-pack security migration tool.\n\n");
    try {
      // deserialize
      final var xPackConfigs = parseConfigs(); // TODO: gracefully handle ConfigValidationException
      System.out.println(xPackConfigs);
      // migrate
      final Migrator migrator = new Migrator();
      final Migrator.MigrationContext context = getMigrationContext(xPackConfigs);
      final List<NamedConfig<?>> searchGuardConfigs = migrator.migrate(context);
      // serialize
      writeConfigs(searchGuardConfigs);
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
        Optional.empty(), // TODO: Get real config
        Optional.ofNullable((XPackElasticsearchConfig) xPackConfigs.get("elasticsearch.yml")),
        Optional.empty() // TODO: Get real config
        );
  }

  private void registerSubMigrators() {
    MigratorRegistry.registerSubMigratorStatic(new AuthMigrator());

    MigratorRegistry.finalizeMigratorsStatic(); // Never forget
  }

  private Map<String, Object> parseConfigs()
      throws SgctlException, IOException, ConfigValidationException {
    final Map<String, Object> configs = new HashMap<>();
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
      final var parsed = parserFunction.parse(config, Parser.Context.get());
      configs.put(configFileName, parsed);
    }
    return configs;
  }

  private void writeConfigs(List<NamedConfig<?>> configs) throws IOException, SgctlException {
    if (!Files.exists(outputDir)) {
      Files.createDirectory(outputDir);
    }

    for (final var config : configs) {
      final String configFileName = config.getFileName();
      final Object configObj = config.toBasicObject();
      final Path configPath = outputDir.resolve(config.getFileName());

      if (Files.exists(configPath) && !overwrite) {
        throw new SgctlException("Refusing to overwrite existing config file: " + configFileName);
      }

      DocWriter.yaml().write(configPath.toFile(), configObj);
    }
  }
}
