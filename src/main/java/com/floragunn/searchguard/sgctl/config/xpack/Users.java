package com.floragunn.searchguard.sgctl.config.xpack;

import com.fasterxml.jackson.core.JsonFactory;
import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;



public record Users(
        ImmutableMap<String, User> mappings
) {


    public static Users parse(DocNode config, Parser.Context parserContext) throws ConfigValidationException {
        DocNode filteredConfig = config.getAsNode("hits","hits");
        ValidationErrors sharedErrors = new ValidationErrors();
        ValidatingDocNode rootVNode = new ValidatingDocNode(filteredConfig, sharedErrors, parserContext);
        var builder = new ImmutableMap.Builder<String, User>(filteredConfig.toListOfNodes().size());

        for(DocNode entry: filteredConfig.toListOfNodes()) {
            ValidatingDocNode sourceNode = new ValidatingDocNode(entry,sharedErrors,parserContext);
            User user = sourceNode.get("_source").by(User::parse);
            if(user != null) builder.with(user.username(), user);
        }

        rootVNode.throwExceptionForPresentErrors();
        return new Users(builder.build());
    }

    public record User(
            String username,
            String password,
            ImmutableList<String> roles,
            ImmutableMap<String, Object> metadata,
            String email
    ) {

        public static User parse(DocNode config, Parser.Context parserContext) throws ConfigValidationException{
            ValidationErrors vErrors = new ValidationErrors();
            ValidatingDocNode vNode = new ValidatingDocNode(config, vErrors, parserContext);

            if(!vNode.get("enabled").asBoolean()) return null;

            User user = new User(
                    vNode.get("username").required().asString(),
                    vNode.get("password").required().asString(),
                    vNode.get("roles").required().asListOfStrings(),
                    vNode.get("metadata").required().asMap(),
                    vNode.get("email").asString()
            );

            vErrors.throwExceptionForPresentErrors();
            return user;


        }

    }

}
