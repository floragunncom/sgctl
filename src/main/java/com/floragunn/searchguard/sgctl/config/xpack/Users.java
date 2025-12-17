package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import java.util.Objects;

public record Users(Traceable<ImmutableMap<String, User>> users) {

  public Users {
    Objects.requireNonNull(users, "users must not be null");
  }

  public static Users parse(DocNode config, Parser.Context parserContext)
      throws ConfigValidationException {

    var errors = new ValidationErrors();
    // ValidatingDocNode vNode = new ValidatingDocNode(config, errors, parserContext);
    var tDoc = TraceableDocNode.of(config, new Source.Config("users.json"), errors);
    var builder = new ImmutableMap.Builder<String, User>(config.toListOfNodes().size());

    for (String name : config.keySet()) {
      User user = tDoc.get(name).as(User::parse).getValue();
      if (user != null) {
        builder.with(name, user);
      }
    }

    errors.throwExceptionForPresentErrors();
    return new Users(Traceable.of(tDoc.getSource(), builder.build()));
  }

  public record User(
      Traceable<String> username,
      Traceable<ImmutableList<Traceable<String>>> roles,
      Traceable<ImmutableMap<String, Traceable<String>>>
          metadata) { // metadata zu string string geändert, sonst hats irgendwie nicht geklappt

    public static User parse(TraceableDocNode tDoc) {

      if (!tDoc.get("enabled").asBoolean().getValue()) return null;

      var username = tDoc.get("username").required().asString();
      var roles = tDoc.get("roles").required().asListOfStrings();

      var metadataBuilder = new ImmutableMap.Builder<String, Traceable<String>>();
      var metadataMap = tDoc.get("metadata").asMapOfStrings();
      for (var entry : metadataMap.getValue().entrySet()) {
        metadataBuilder.with(entry.getKey(), entry.getValue());
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

      return user;
    }
  }
}
