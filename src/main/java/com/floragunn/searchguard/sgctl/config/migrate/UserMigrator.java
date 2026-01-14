package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalUsers;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import java.util.List;

public class UserMigrator implements SubMigrator {

  @Override
  public List<NamedConfig<?>> migrate(
      Migrator.IMigrationContext context, MigrationReporter reporter) {

    var xpackUsers = context.getUsers();
    if (xpackUsers.isEmpty()) {
      reporter.problem("users.json is empty");
      return List.of();
    }

    var builder =
        new ImmutableList.Builder<SgInternalUsers.User>(xpackUsers.get().users().get().size());

    for (var entry : xpackUsers.get().users().get().entrySet()) {

      var user = entry.getValue().get();

      // skip if disabled
      if (!user.enabled().get()) {
        continue;
      }
      // convert ImmutableMap<String, Traceable<String>> from xpack to ImmutableMap<String, String>
      // for searchguard
      ImmutableMap<String, String> xPackMetaData =
          !user.metadata().get().isEmpty()
              ? user.metadata().get().map(key -> key, Traceable::get)
              : ImmutableMap.empty();

      var metadataBuilder =
          new ImmutableMap.Builder<String, String>(
              xPackMetaData.size() + 3); // 3 potenzielle extra metadaten

      for (var metadataEntry : xPackMetaData.entrySet()) {
        metadataBuilder.put(metadataEntry.getKey(), metadataEntry.getValue());
      }

      if (user.fullName().get().isPresent()) {
        metadataBuilder.put("full_name", user.fullName().getValue());
      }

      if (user.email().get().isPresent()) {
        metadataBuilder.put("email", user.email().getValue());
      }

      if (user.profileUid().get().isPresent()) {
        metadataBuilder.put("profileUid", user.profileUid().getValue());
      }

      ImmutableMap<String, String> sgMetaData = metadataBuilder.build();

      builder.add(
          new SgInternalUsers.User(
              user.username().get(), "", user.roles().get().map(Traceable::get), sgMetaData));
    }

    reporter.problem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");

    return List.of(new SgInternalUsers(builder.build()));
  }
}
