package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalUsers;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserMigrator implements SubMigrator {
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {

    Optional<Users> xpackUsers = context.getUsers();
    if (xpackUsers.isEmpty() || xpackUsers.get().users().get().isEmpty()) {
      reporter.problem("users.json is empty");
      return List.of();
    }

    var builder =
        new ImmutableList.Builder<SgInternalUsers.User>(xpackUsers.get().users().get().size());

    for (Map.Entry<String, Users.User> entry : xpackUsers.get().users().get().entrySet()) {

      // convert ImmutableMap<String, Traceable<String>> from xpack to ImmutableMap<String, String>
      // for searchguard
      ImmutableMap<String, String> sgMetaData =
          entry.getValue().metadata() != null
              ? entry.getValue().metadata().get().map(key -> key, Traceable::get)
              : ImmutableMap.empty();

      builder.add(
          new SgInternalUsers.User(
              entry.getValue().username().get(),
              "",
              entry.getValue().roles().get().map(Traceable::get),
              entry.getValue().metadata().get().map(key -> key, Traceable::get)));
    }

    reporter.problem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");

    return List.of(new SgInternalUsers(builder.build()));
  }
}
