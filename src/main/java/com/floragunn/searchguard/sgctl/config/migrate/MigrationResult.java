package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;

/**
 * The result of the migration process.
 *
 * @param configs The migrated configurations.
 * @param report The migration report.
 */
public record MigrationResult(ImmutableList<NamedConfig<?>> configs, String report) {}
