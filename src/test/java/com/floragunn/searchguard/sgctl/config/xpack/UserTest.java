package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.xpack.Users.User;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class UserTest {

  final Users exampleUsers =
      new Users(
          ImmutableMap.of(
              "john_doe",
              new User(
                  "john_doe",
                  "$2a$12$QzOf63.lc/QaesXkvmk6DOrmmaL001QqMJ403CoDZPgPjLnag/PQC",
                  ImmutableList.of("admin", "monitoring"),
                  ImmutableMap.of("department", "IT", "employee_id", "A1234"),
                  "john.doe@example.com")));

  @Test
  public void parseExampleTest() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/example.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());
    assertEquals(exampleUsers, parsedUsers);
  }

  @Test
  public void parseInvalidMissingPassword() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/missing_password.json");

    assertThrows(ConfigValidationException.class, () -> Users.parse(node, Parser.Context.get()));
  }

  @Test
  public void multipleUsers() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/multipleUsers.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());

    assertEquals(
        new Users(
            ImmutableMap.of(
                "john_doe",
                new User(
                    "john_doe",
                    "$2a$12$QzOf63.lc/QaesXkvmk6DOrmmaL001QqMJ403CoDZPgPjLnag/PQC",
                    ImmutableList.of("admin", "monitoring"),
                    ImmutableMap.of("department", "IT", "employee_id", "A1234"),
                    "john.doe@example.com"),
                "jane_smith",
                new User(
                    "jane_smith",
                    "$2a$12$oej89fF9fXQleZtlmZklrOk39uOcPu3dfsh9FJLpuaYDoG0k6npqG",
                    ImmutableList.of("kibana_admin", "other_role2"),
                    ImmutableMap.of("department", "Marketing", "employee_id", "B8891"),
                    "jane.smith@example.com"),
                "pauls_lee",
                new User(
                    "pauls_lee",
                    "$2a$12$xyz123456789abcdefghijklmnopqrs",
                    ImmutableList.of("viewer"),
                    ImmutableMap.of("department", "Support", "employee_id", "C3345"),
                    "paul.lee@example.com"),
                "lisa_kim",
                new User(
                    "lisa_kim",
                    "$2a$12$abcd1234efgh5678ijklmnopqrstuv",
                    ImmutableList.of("admin", "kibana_admin"),
                    ImmutableMap.of("department", "IT", "employee_id", "D5566"),
                    "lisa.kim@example.com"),
                "mark_tan",
                new User(
                    "mark_tan",
                    "$2a$12$mnop1234qrst5678uvwxabcdefghi",
                    ImmutableList.of("developer"),
                    ImmutableMap.of("department", "R&D", "employee_id", "E7788"),
                    "mark.tan@example.com"))),
        parsedUsers);
  }

  @Test
  public void parseDisabledUser() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/disabledUser.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());

    assertEquals(
        new Users(
            ImmutableMap.of(
                "john_doe",
                new User(
                    "john_doe",
                    "$2a$12$QzOf63.lc/QaesXkvmk6DOrmmaL001QqMJ403CoDZPgPjLnag/PQC",
                    ImmutableList.of("admin", "monitoring"),
                    ImmutableMap.of(
                        "department", "IT",
                        "employee_id", "A1234"),
                    "john.doe@example.com"),
                "jane_smith",
                new User(
                    "jane_smith",
                    "$2a$12$oej89fF9fXQleZtlmZklrOk39uOcPu3dfsh9FJLpuaYDoG0k6npqG",
                    ImmutableList.of("kibana_admin", "other_role2"),
                    ImmutableMap.of(
                        "department", "Marketing",
                        "employee_id", "B8891"),
                    "jane.smith@example.com"))),
        parsedUsers);
  }

  @Test
  public void parseEmptyHits() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/emptyHits.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());
    assertEquals(new Users(ImmutableMap.empty()), parsedUsers);
  }

  @Test
  public void parseMissingMultiple() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/missingMultiple.json");

    ConfigValidationException exception =
        assertThrows(
            ConfigValidationException.class, () -> Users.parse(node, Parser.Context.get()));
    assertTrue(exception.getValidationErrors().size() >= 2);
  }

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = UserTest.class.getResourceAsStream(path)) {
      assertNotNull(in);
      return DocNode.wrap(DocReader.json().read(in));
    }
  }
}
