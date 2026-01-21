package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Class for migrating parsed XPack config to SearchGuard configs using {@link SubMigrator}s. */
public class Migrator {

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
    System.out.println("─── Migration");

    final Map<String, NamedConfig<?>> migratedConfigs = new HashMap<>();
    final List<SubMigrator> subMigrators;
    try {
      subMigrators = MigratorRegistry.getInstance().getSubMigrators();
    } catch (IllegalStateException e) {
      // TODO: maybe better handling?
      System.err.println("Migrator registry has not been finalized!");
      throw e;
    }

    for (final SubMigrator subMigrator : subMigrators) {
      final List<NamedConfig<?>> migratedSubConfigs = subMigrator.migrate(context, reporter);

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

    // Check for critical problems BEFORE announcing success
    if (reporter.hasCriticalProblems()) {
      // Don't print "Finished migration" - the migration failed
      return new MigrationResult.Failure(
          reporter.generateReport(), reporter.generateReportSummary());
    }

    System.out.println("✓ Migration completed - " + migratedConfigs.size() + " file(s) generated");
    var outputMigratedConfigsBuilder = new ImmutableList.Builder<NamedConfig<?>>();

    for (NamedConfig<?> migratedConfig : migratedConfigs.values()) {
      outputMigratedConfigsBuilder.add(migratedConfig);
    }

    // No critical problems at this point, return Success
    return new MigrationResult.Success(
        outputMigratedConfigsBuilder.build(),
        reporter.generateReport(),
        reporter.generateReportSummary());
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
