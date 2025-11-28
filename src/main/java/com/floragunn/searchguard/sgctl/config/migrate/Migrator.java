package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrator {

  Logger logger = LoggerFactory.getLogger(Migrator.class);

  public List<NamedConfig<?>> migrate(@Nullable RoleMappings roleMappings, @Nullable Roles roles) {
    logger.info("Starting config migration!");
    List<NamedConfig<?>> migrated = new ArrayList<>();
    // TODO: Migrate with sub migrators
    logger.info("Ending config migration!");
    return migrated;
  }
}
