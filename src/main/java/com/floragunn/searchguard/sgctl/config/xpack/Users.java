package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

public record Users(ImmutableMap<String, User> mappings) {

  public static Users parse(DocNode config, Parser.Context parserContext)
      throws ConfigValidationException {

    ValidatingDocNode vNode = new ValidatingDocNode(config, new ValidationErrors(), parserContext);
    var builder = new ImmutableMap.Builder<String, User>(config.toListOfNodes().size());

    for (String name : config.keySet()) {
      User user = vNode.get(name).by(Users.User::parse);
      if (user != null) builder.with(name, user);
    }

    vNode.throwExceptionForPresentErrors();
    return new Users(builder.build());
  }

  public record User(
      String username, ImmutableList<String> roles, ImmutableMap<String, Object> metadata) {

    public static User parse(DocNode config, Parser.Context parserContext)
        throws ConfigValidationException {
      ValidationErrors vErrors = new ValidationErrors();
      ValidatingDocNode vNode = new ValidatingDocNode(config, vErrors, parserContext);

      if (!vNode.get("enabled").asBoolean()) return null;

      ImmutableMap<String, Object> metadata = vNode.get("metadata").required().asMap();

      String fullName = vNode.get("full_name").asString();
      if (fullName != null) {
        metadata = metadata.with("full_name", fullName);
      }

      String email = vNode.get("email").asString();
      if (email != null) {
        metadata = metadata.with("email", email);
      }

      String profileUid = vNode.get("profile_uid").asString();
      if (profileUid != null) {
        metadata = metadata.with("profile_uid", profileUid);
      }

      User user =
          new User(
              vNode.get("username").required().asString(),
              vNode.get("roles").required().asListOfStrings(),
              metadata);

      vErrors.throwExceptionForPresentErrors();
      return user;
    }
  }
}
