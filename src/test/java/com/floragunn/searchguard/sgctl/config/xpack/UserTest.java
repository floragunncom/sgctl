package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void parseExampleTest() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/example.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());

    assertNotNull(parsedUsers);
    assertNotNull(parsedUsers.users());
    assertTrue(parsedUsers.users().get().containsKey("john_doe"));

    var user = parsedUsers.users().get().get("john_doe");
    assertNotNull(user);

    assertNotNull(user.get().username());
    assertEquals("john_doe", user.get().username().get());

    assertNotNull(user.get().roles());
    assertEquals(2, user.get().roles().get().size());
    assertEquals("admin", user.get().roles().get().get(0).get());
    assertEquals("monitoring", user.get().roles().get().get(1).get());

    assertNotNull(user.get().metadata());
    var metadata = user.get().metadata().get();
    assertNotNull(metadata);

    assertTrue(metadata.containsKey("department"));
    assertEquals("IT", metadata.get("department").get());

    assertTrue(metadata.containsKey("employee_id"));
    assertEquals("A1234", metadata.get("employee_id").get());
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

    assertNotNull(parsedUsers);
    assertNotNull(parsedUsers.users());
    var users = parsedUsers.users().get();
    assertEquals(5, users.size());

    assertTrue(users.containsKey("john_doe"));
    var john = users.get("john_doe");
    assertNotNull(john);
    assertEquals("john_doe", john.get().username().get());
    assertEquals(2, john.get().roles().get().size());

    assertTrue(users.containsKey("jane_smith"));
    var jane = users.get("jane_smith");
    assertNotNull(jane);
    assertEquals("jane_smith", jane.get().username().get());
    assertEquals(2, jane.get().roles().get().size());

    assertTrue(users.containsKey("pauls_lee"));
    var paul = users.get("pauls_lee");
    assertNotNull(paul);
    assertEquals("pauls_lee", paul.get().username().get());
    assertEquals(1, paul.get().roles().get().size());

    assertTrue(users.containsKey("lisa_kim"));
    var lisa = users.get("lisa_kim");
    assertNotNull(lisa);
    assertEquals("lisa_kim", lisa.get().username().get());
    assertEquals(2, lisa.get().roles().get().size());

    assertTrue(users.containsKey("mark_tan"));
    var mark = users.get("mark_tan");
    assertNotNull(mark);
    assertEquals("mark_tan", mark.get().username().get());
    assertEquals(1, mark.get().roles().get().size());
  }

  @Test
  public void parseEmptyHits() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/emptyHits.json");
    Users parsedUsers = Users.parse(node, Parser.Context.get());
    assertNotNull(parsedUsers);
    assertNotNull(parsedUsers.users());
    assertTrue(parsedUsers.users().get().isEmpty());
  }

  @Test
  public void parseMissingMultiple() throws IOException, ConfigValidationException {
    DocNode node = read("/xpack_migrate/users/missingMultiple.json");

    ConfigValidationException exception =
        assertThrows(
            ConfigValidationException.class, () -> Users.parse(node, Parser.Context.get()));

    assertNotNull(exception);
  }

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = UserTest.class.getResourceAsStream(path)) {
      assertNotNull(in);
      return DocNode.wrap(DocReader.json().read(in));
    }
  }
}
