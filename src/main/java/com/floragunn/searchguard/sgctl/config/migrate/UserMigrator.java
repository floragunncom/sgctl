package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalUsers;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

public class UserMigrator implements SubMigrator {

  public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
      throws SgctlException {
    logger.info("Migrating Users"); // needed ?
    Optional<Users> xpackUsers = context.getUsers();
    if (xpackUsers.isEmpty()) {
      logger.warn("users.json is empty");
      return List.of();
    }

    var builder =
        new ImmutableList.Builder<SgInternalUsers.User>(xpackUsers.get().mappings().size());

    for (Map.Entry<String, Users.User> entry : xpackUsers.get().mappings().entrySet()) {

      // convert ImmutableMap<String, Object> from xpack to ImmutableMap<String, String> for search
      // guard
      ImmutableMap<String, String> sgMetaData =
          entry.getValue().metadata() != null
              ? entry.getValue().metadata().map(key -> key, value -> String.valueOf(value))
              : ImmutableMap.empty();

      builder.add(
          new SgInternalUsers.User(
              entry.getValue().username(), "", entry.getValue().roles(), sgMetaData));
    }

    logger.warn(
        "Passwords are empty for all migrated users. Each user must reset their password or a admin nmust set them manually.");
    return List.of(new SgInternalUsers(builder.build()));
  }
}
