package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.*;
import java.util.Objects;

public record Users(Traceable<ImmutableMap<String, User>> users) {

  public Users {
    Objects.requireNonNull(users, "users must not be null");
  }

  public static Users parse(DocNode config, Parser.Context parserContext)
      throws ConfigValidationException {

    var errors = new ValidationErrors();
    var tDoc = TraceableDocNode.of(config, new Source.Config("users.json"), errors);
    var builder = new ImmutableMap.Builder<String, User>(config.toListOfNodes().size());

    for (String name : config.keySet()) {
      var user = tDoc.get(name).as(User::parse).getValue();
      if (!user.get().isEmpty()) {
        builder.with(name, user.getValue());
      }
    }

    tDoc.throwExceptionForPresentErrors();
    return new Users(Traceable.of(tDoc.getSource(), builder.build()));
  }

  public record User(
      Traceable<String> username,
      Traceable<ImmutableList<Traceable<String>>> roles,
      Traceable<ImmutableMap<String, Traceable<String>>>
          metadata) { // metadata zu string string geändert, sonst hats irgendwie nicht geklappt

    public static OptTraceable<User> parse(TraceableDocNode tDoc) {

      if (!tDoc.get("enabled").asBoolean().getValue()) {
        return OptTraceable.empty(tDoc.getSource());
      }

      var username = tDoc.get("username").required().asString();
      var roles = tDoc.get("roles").required().asListOfStrings();

      var metadataBuilder = new ImmutableMap.Builder<String, Traceable<String>>();
      var metadataMap = tDoc.get("metadata").required().asMapOfStrings();
      if (!metadataMap.get().isEmpty()) {
        for (var entry : metadataMap.get().entrySet()) {
          metadataBuilder.with(entry.getKey(), entry.getValue());
        }
      }

      var fullName = tDoc.get("full_name").asString().orElse(null);
      if (fullName != null) {
        metadataBuilder.with("full_name", fullName);
      }

      var email = tDoc.get("email").asString().orElse(null);
      if (email != null) {
        metadataBuilder.with("email", email);
      }

      var profileUid = tDoc.get("profileUid").asString().orElse(null);
      if (profileUid != null) {
        metadataBuilder.with("profileUid", profileUid);
      }

      var metadata = metadataBuilder.build();

      User user = new User(username, roles, Traceable.of(tDoc.getSource(), metadata));

      return OptTraceable.of(tDoc.getSource(), user);
    }
  }
}
