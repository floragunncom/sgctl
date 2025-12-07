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

  @Test
  public void parseExampleTest() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/example.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());

    assertEquals(
        new Users(
            ImmutableMap.of(
                "john_doe",
                new User(
                    "john_doe",
                    ImmutableList.of("admin", "monitoring"),
                    ImmutableMap.<String, Object>of(
                        "department", "IT",
                        "employee_id", "A1234",
                        "full_name", "John Doe",
                        "email", "john.doe@example.com")))),
        parsedUsers);
  }

  @Test
  public void parseMissingMetadata() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/missing_metadata.json");

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
                    ImmutableList.of("admin", "monitoring"),
                    ImmutableMap.<String, Object>of(
                        "department", "IT",
                        "employee_id", "A1234",
                        "full_name", "John Doe",
                        "email", "john.doe@example.com")),
                "jane_smith",
                new User(
                    "jane_smith",
                    ImmutableList.of("kibana_admin", "other_role2"),
                    ImmutableMap.<String, Object>of(
                        "department", "Marketing",
                        "employee_id", "B8891",
                        "full_name", "Jane Smith",
                        "email", "jane.smith@example.com")),
                "pauls_lee",
                new User(
                    "pauls_lee",
                    ImmutableList.of("viewer"),
                    ImmutableMap.<String, Object>of(
                        "department", "Support",
                        "employee_id", "C3345",
                        "full_name", "Paul Lee",
                        "email", "paul.lee@example.com")),
                "lisa_kim",
                new User(
                    "lisa_kim",
                    ImmutableList.of("admin", "kibana_admin"),
                    ImmutableMap.<String, Object>of(
                        "department", "IT",
                        "employee_id", "D5566",
                        "full_name", "Lisa Kim",
                        "email", "lisa.kim@example.com")),
                "mark_tan",
                new User(
                    "mark_tan",
                    ImmutableList.of("developer"),
                    ImmutableMap.<String, Object>of(
                        "department", "R&D",
                        "employee_id", "E7788",
                        "full_name", "Mark Tan",
                        "email", "mark.tan@example.com")))),
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
                    ImmutableList.of("admin", "monitoring"),
                    ImmutableMap.<String, Object>of(
                        "department", "IT",
                        "employee_id", "A1234",
                        "full_name", "John Doe",
                        "email", "john.doe@example.com")),
                "jane_smith",
                new User(
                    "jane_smith",
                    ImmutableList.of("kibana_admin", "other_role2"),
                    ImmutableMap.<String, Object>of(
                        "department", "Marketing",
                        "employee_id", "B8891",
                        "full_name", "Jane Smith",
                        "email", "jane.smith@example.com")))),
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
