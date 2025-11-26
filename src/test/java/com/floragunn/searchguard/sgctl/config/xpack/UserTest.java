package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.xpack.Users.User;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class UserTest {

    final Users exampleUsers = new Users(ImmutableMap.of(
            "john_doe",
            new User(
                    "john_doe",
                    "$2a$12$QzOf63.lc/QaesXkvmk6DOrmmaL001QqMJ403CoDZPgPjLnag/PQC",
                    ImmutableList.of("admin","other_role1"),
                    ImmutableMap.of("custom_attribute", "xyz")
            )
    ));

    @Test
    public void parseExampleTest() throws IOException, ConfigValidationException {
        DocNode node = read("/xpack_migrate/users/example.json");
        Users parsedUsers = Users.parse(node, Parser.Context.get());

        assertEquals(
                exampleUsers,
                parsedUsers
                );
    }



    @Test
    public void parseInvalidMissingAttribute() throws IOException, ConfigValidationException {
        DocNode node = read("/xpack_migrate/users/missing_hash.json");
        assertThrows(ConfigValidationException.class, () -> Users.parse(node, Parser.Context.get()));
    }

    @Test
    public void parseMultipleUsers() throws IOException, ConfigValidationException {
        DocNode node = read("/xpack_migrate/users/multipleUsers.json");
        Users parsedUsers = Users.parse(node, Parser.Context.get());

        assertEquals(
                new Users(ImmutableMap.of(
                        "john_doe",
                        new User(
                                "john_doe",
                                "$2a$12$QzOf63.lc/QaesXkvmk6DOrmmaL001QqMJ403CoDZPgPjLnag/PQC",
                                ImmutableList.of("admin","other_role1"),
                                ImmutableMap.of("custom_attribute", "xyz")
                        ),
                        "not_john_doe",
                        new User(
                                "not_john_doe",
                                "different hash",
                                ImmutableList.of("normal-user","other_role2"),
                                ImmutableMap.of("different_custom_attribute", "zyx")
                        )

                )),
                parsedUsers
        );
    }


    private DocNode read(String path) throws IOException, DocumentParseException {
        try (var in = RoleTest.class.getResourceAsStream(path)) {
            assertNotNull(in);
            return DocNode.wrap(DocReader.json().read(in));
        }
    }

}
