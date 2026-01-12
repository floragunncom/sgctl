package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.*;
import java.util.Objects;

public record Users(Traceable<ImmutableMap<String, Traceable<User>>> users) {

  public Users {
    Objects.requireNonNull(users, "users must not be null");
  }

  public static Users parse(DocNode config, Parser.Context parserContext)
      throws ConfigValidationException {
    var tDoc = TraceableDocNode.of(config, new Source.Config("users.json"));

    var users = tDoc.asAttribute().asMapOf(User::parse);

    tDoc.throwExceptionForPresentErrors();
    return new Users(users);
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
