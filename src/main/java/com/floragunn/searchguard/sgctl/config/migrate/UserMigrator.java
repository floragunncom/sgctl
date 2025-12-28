package com.floragunn.searchguard.sgctl.config.migrate;

public class UserMigrator implements SubMigrator {
  /*public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {

    return users;
  }*/

  /*
  public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
      throws SgctlException {
    logger.info("Migrating Users"); // needed ?
    Optional<Users> xpackUsers = context.getUsers();
    if (xpackUsers.isEmpty()) {
      logger.warn("users.json is empty");
      return List.of();
    }

    var builder =
        new ImmutableList.Builder<SgInternalUsers.User>(xpackUsers.get().users().get().size());

    for (Map.Entry<String, Users.User> entry : xpackUsers.get().users().get().entrySet()) {

      // convert ImmutableMap<String, Object> from xpack to ImmutableMap<String, String> for search
      // guard
      /*ImmutableMap<String, String> sgMetaData =
      entry.getValue().metadata() != null
          ? entry.getValue().metadata().map(key -> key, value -> String.valueOf(value))
          : ImmutableMap.empty();

      builder.add(
          new SgInternalUsers.User(
              entry.getValue().username().get(),
              "",
              entry.getValue().roles().get().map(Traceable::get),
              entry.getValue().metadata().get().map(key -> key, Traceable::get)));
    }

    logger.warn(
        "Passwords are empty for all migrated users. Each user must reset their password or a admin must set them manually.");
    return List.of(new SgInternalUsers(builder.build()));
  }*/
}
