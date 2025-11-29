package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.google.common.collect.ImmutableList;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for migrating parsed XPack config to SearchGuard configs using {@link SubMigrator}s. */
public class Migrator {

  Logger logger = LoggerFactory.getLogger(Migrator.class);

  /**
   * Executes all {@link SubMigrator}s added to the {@link MigratorRegistry} using
   * registerSubMigratorStatic()
   *
   * @param context All parsed XPackConfigs. Gets passed to the subMigrators
   * @return A List of SearchGuard Configs
   */
  public List<NamedConfig<?>> migrate(IMigrationContext context) throws SgctlException {
    logger.info("Starting migration");

    final Map<String, NamedConfig<?>> migratedConfigs = new HashMap<>();
    final List<SubMigrator> subMigrators;
    try {
      subMigrators = MigratorRegistry.getSubMigratorsStatic();
    } catch (IllegalStateException e) {
      // TODO: maybe better handling?
      logger.warn("Migrator registry has not been finalized!");
      throw e;
    }

    for (final SubMigrator subMigrator : subMigrators) {
      logger.debug("Running migration with {}", subMigrator.getClass().getSimpleName());
      final List<NamedConfig<?>> migratedSubConfigs = subMigrator.migrate(context, logger);
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
    var outputMigratedConfigsBuilder = ImmutableList.<NamedConfig<?>>builder();

    for (NamedConfig<?> migratedConfig : migratedConfigs.values()) {
      outputMigratedConfigsBuilder.add(migratedConfig);
    }

    return outputMigratedConfigsBuilder.build();
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
      @NotNull Optional<?> users,
      @NotNull Optional<?> elasticSearch,
      @NotNull Optional<?> kibana)
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
    public Optional<?> getUsers() {
      return users;
    }

    @Override
    public Optional<?> getElasticsearch() {
      return elasticSearch;
    }

    @Override
    public Optional<?> getKibana() {
      return kibana;
    }
  }

  public interface IMigrationContext {

    Optional<RoleMappings> getRoleMappings();

    Optional<Roles> getRoles();

    Optional<?> getUsers(); // TODO: Add real type when merged

    Optional<?> getElasticsearch(); // TODO: Add real type when merged

    Optional<?> getKibana(); // TODO: Add real type or remove if unneeded
  }
}
