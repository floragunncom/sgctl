package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
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

    //TODO: jq search query von sg blog 3 hier umsetzen, damit Nutzer nicht jq nutzen muss ?
    //username ist der key, wird aber auch an User parse übergeben, damit User ein username attribut hat, vielleicht leichter zum migrieren
    public static Users parse(DocNode config, Parser.Context parserContext) throws ConfigValidationException {
        ValidatingDocNode vNode = new ValidatingDocNode(config, new ValidationErrors(), parserContext);

        var builder = new ImmutableMap.Builder<String, User>(config.size());
        for(String name: config.keySet()) {
            //testen ob das funktioniert, ansonsten hat User kein username attribut und beim migrieren muss darauf geachtet werden (ist dann der key selbst)
            User user = vNode.get(name).by(node -> User.parse(node, parserContext, name));
            builder.with(name, user);
        }

        vNode.throwExceptionForPresentErrors();
        return new Users(builder.build());

    }

    public record User(
            String username,
            String password,
            ImmutableList<String> roles,
            ImmutableMap<String, Object> attributes

            //String email,
            //String full_name,
            //boolean enabled,
            //String profile_uid
    ) {

        //TODO: brauchen vielleicht beispiele, falls ein attribut vielleicht doch nicht required ist
        public static User parse(DocNode config, Parser.Context parserContext, String username) throws ConfigValidationException{
            ValidationErrors vErrors = new ValidationErrors();
            ValidatingDocNode vNode = new ValidatingDocNode(config, vErrors, parserContext);

            User user = new User(
                    username,
                    vNode.get("hash").required().asString(),
                    vNode.get("opendistro_security_roles").required().asListOfStrings(),
                    vNode.get("attributes").required().asMap()
            );

            vErrors.throwExceptionForPresentErrors();
            return user;


        }

    }

}
