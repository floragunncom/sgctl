package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import java.util.List;
import org.slf4j.Logger;

/** A sub part for the Migrator (SubMigrator) */
public interface SubMigrator {
  /**
   * @param context Contains all the parsed XPack configs
   * @param logger Logger for logging
   * @return Migrated SearchGuard configs
   */
  List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
          throws SgctlException;
}
