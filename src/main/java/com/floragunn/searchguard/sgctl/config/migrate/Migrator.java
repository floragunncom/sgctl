package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import com.floragunn.searchguard.sgctl.config.xpack.Kibana;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import com.floragunn.searchguard.sgctl.config.xpack.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for migrating parsed XPack config to SearchGuard configs using {@link SubMigrator}s. */
public class Migrator {

  private final Logger logger = LoggerFactory.getLogger(Migrator.class);
  private final MigrationReporter reporter;

  public Migrator(MigrationReporter reporter) {
    this.reporter = reporter;
  }

  public Migrator() {
    this(MigrationReporter.searchGuard());
  }

  /**
   * Executes all {@link SubMigrator}s added to the {@link MigratorRegistry} using
   * registerSubMigrator()
   *
   * @param context All parsed XPackConfigs. Gets passed to the subMigrators
   * @return A List of SearchGuard Configs
   */
  public MigrationResult migrate(IMigrationContext context) throws SgctlException {
    logger.info("Starting migration");

    final Map<String, NamedConfig<?>> migratedConfigs = new HashMap<>();
    final List<SubMigrator> subMigrators;
    try {
      subMigrators = MigratorRegistry.getInstance().getSubMigrators();
    } catch (IllegalStateException e) {
      // TODO: maybe better handling?
      logger.warn("Migrator registry has not been finalized!");
      throw e;
    }

    for (final SubMigrator subMigrator : subMigrators) {
      logger.debug("Running migration with {}", subMigrator.getClass().getSimpleName());
      final List<NamedConfig<?>> migratedSubConfigs = subMigrator.migrate(context, reporter);
      logger.debug("SubMigrator returned {} config files", migratedSubConfigs.size());

      for (NamedConfig<?> migratedSubConfig : migratedSubConfigs) {
        if (migratedConfigs.containsKey(migratedSubConfig.getFileName())) {
          throw new IllegalStateException(
              String.format(
                  "Migrator %s attempted to output file '%s' that was already migrated by another subMigrator!",
                  subMigrator.getClass().getName(), migratedSubConfig.getFileName()));
        }
        migratedConfigs.put(migratedSubConfig.getFileName(), migratedSubConfig);
      }
    }

    logger.info("Finished migration with {} files", migratedConfigs.size());
    var outputMigratedConfigsBuilder = new ImmutableList.Builder<NamedConfig<?>>();

    for (NamedConfig<?> migratedConfig : migratedConfigs.values()) {
      outputMigratedConfigsBuilder.add(migratedConfig);
    }

    if (reporter.hasCriticalProblems()) {
      return new MigrationResult.Failure(
          reporter.generateReport(), reporter.generateReportSummary());
    } else {
      return new MigrationResult.Success(
          outputMigratedConfigsBuilder.build(),
          reporter.generateReport(),
          reporter.generateReportSummary());
    }
  }

  /**
   * All parsed XPack Configs
   *
   * @param roleMappings
   * @param roles
   */
  public record MigrationContext(
      @NotNull Optional<RoleMappings> roleMappings,
      @NotNull Optional<Roles> roles,
      @NotNull Optional<Users> users,
      @NotNull Optional<XPackElasticsearchConfig> elasticSearch,
      @NotNull Optional<Kibana> kibana)
      implements IMigrationContext {

    @Override
    public Optional<RoleMappings> getRoleMappings() {
      return roleMappings;
    }

    @Override
    public Optional<Roles> getRoles() {
      return roles;
    }

    @Override
    public Optional<Users> getUsers() {
      return users;
    }

    @Override
    public Optional<XPackElasticsearchConfig> getElasticsearch() {
      return elasticSearch;
    }

    @Override
    public Optional<Kibana> getKibana() {
      return kibana;
    }
  }

  public interface IMigrationContext {

    Optional<RoleMappings> getRoleMappings();

    Optional<Roles> getRoles();

    Optional<Users> getUsers();

    Optional<XPackElasticsearchConfig> getElasticsearch();

    Optional<Kibana> getKibana();
  }
}
