package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.OptTraceable;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import java.util.Objects;

public record Users(Traceable<ImmutableMap<String, Traceable<User>>> users) {

  public Users {
    Objects.requireNonNull(users, "users must not be null");
  }

  public static Users parse(TraceableDocNode tDoc) {
    return new Users(tDoc.asAttribute().asMapOf(User::parse));
  }

  public record User(
      Traceable<String> username,
      Traceable<ImmutableList<Traceable<String>>> roles,
      Traceable<ImmutableMap<String, Traceable<String>>> metadata,
      Traceable<Boolean> enabled,
      OptTraceable<String> fullName,
      OptTraceable<String> email,
      OptTraceable<String> profileUid) {

    public static User parse(TraceableDocNode tDoc) {

      var enabled = tDoc.get("enabled").required().asBoolean();

      var username = tDoc.get("username").required().asString();
      var roles = tDoc.get("roles").required().asListOfStrings();

      var metadata = tDoc.get("metadata").required().asMapOfStrings();

      var fullName = tDoc.get("full_name").asString();
      var email = tDoc.get("email").asString();
      var profileUid = tDoc.get("profile_uid").asString();

      User user = new User(username, roles, metadata, enabled, fullName, email, profileUid);

      return user;
    }
  }
}
