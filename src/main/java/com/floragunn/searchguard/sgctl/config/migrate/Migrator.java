package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.google.common.collect.ImmutableList;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrator {

  Logger logger = LoggerFactory.getLogger(Migrator.class);

  public List<NamedConfig<?>> migrate(MigrationContext context) {
    logger.info("Starting migration");

    final Map<String, NamedConfig<?>> migratedConfigs = new HashMap<>();

    final List<SubMigrator> subMigrators = MigratorRegistry.getSubMigratorsStatic();

    for (final SubMigrator subMigrator : subMigrators) {
      logger.debug("Running migration with {}", subMigrator.getClass().getSimpleName());
      final List<NamedConfig<?>> migratedSubConfigs = subMigrator.migrate(context, logger);
      logger.debug("SubMigrator returned {} config files", migratedSubConfigs.size());

      for (NamedConfig<?> migratedSubConfig : migratedSubConfigs) {
        if (migratedConfigs.containsKey(migratedSubConfig.getFileName())) {
          throw new IllegalStateException(
              String.format(
                  "Migrator %s attempted to output file '%s' that was already migrated by other subMigrator!",
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

  public record MigrationContext(Optional<RoleMappings> roleMappings, Optional<Roles> roles
      // TODO: Add all XPack configs here 😀
      ) {}
}
