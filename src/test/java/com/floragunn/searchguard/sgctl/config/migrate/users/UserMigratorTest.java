package com.floragunn.searchguard.sgctl.config.migrate.users;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.migrate.UserMigrator;
import com.floragunn.searchguard.sgctl.config.migrator.AssertableMigrationReporter;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalUsers;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class UserMigratorTest {

  private static final String PATH = "/xpack_migrate/users/";

  @Test
  public void testMigrateExample() throws Exception {
    var users = loadUsers(PATH + "example.json");
    var expected = loadResourceAsString(PATH + "migrated/example.yml");

    var context = createContext(Optional.of(users));
    var migrator = new UserMigrator();
    var reporter = new AssertableMigrationReporter();

    var resultList = migrator.migrate(context, reporter);

    assertFalse(resultList.isEmpty(), "The result list cannot be empty");
    var result = (SgInternalUsers) resultList.get(0);

    String actualYaml = DocWriter.yaml().writeAsString(result);
    assertEquals(normalize(expected), normalize(actualYaml));

    reporter.assertProblem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");
  }

  @Test
  public void testMigrateMultipleUsers() throws Exception {
    var users = loadUsers(PATH + "multipleUsers.json");
    var expected = loadResourceAsString(PATH + "migrated/multipleUsers.yml");

    var context = createContext(Optional.of(users));
    var migrator = new UserMigrator();
    var reporter = new AssertableMigrationReporter();

    var resultList = migrator.migrate(context, reporter);

    assertFalse(resultList.isEmpty(), "The result list cannot be empty");
    var result = (SgInternalUsers) resultList.get(0);

    String actualYaml = DocWriter.yaml().writeAsString(result);

    assertEquals(normalize(expected), normalize(actualYaml));

    reporter.assertProblem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");
    reporter.assertNoMoreProblems();
  }

  @Test
  public void testDisabledUser() throws Exception {
    var users = loadUsers(PATH + "disabledUser.json");
    var expected = loadResourceAsString(PATH + "migrated/disabledUser.yml");

    var context = createContext(Optional.of(users));
    var migrator = new UserMigrator();
    var reporter = new AssertableMigrationReporter();

    var resultList = migrator.migrate(context, reporter);

    assertFalse(resultList.isEmpty(), "The result list cannot be empty");
    var result = (SgInternalUsers) resultList.get(0);

    String actualYaml = DocWriter.yaml().writeAsString(result);

    assertEquals(normalize(expected), normalize(actualYaml));

    reporter.assertProblem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");
    reporter.assertNoMoreProblems();
  }

  @Test
  public void noUsers() throws Exception {

    var users = loadUsers(PATH + "emptyHits.json");
    var expected = loadResourceAsString(PATH + "migrated/emptyHits.yml");

    var context = createContext(Optional.of(users));
    var migrator = new UserMigrator();
    var reporter = new AssertableMigrationReporter();

    var resultList = migrator.migrate(context, reporter);

    assertFalse(
        resultList.isEmpty(), "Result list should contain the empty SgInternalUsers object");
    var result = (SgInternalUsers) resultList.get(0);

    String actualYaml = DocWriter.yaml().writeAsString(result);

    assertEquals(normalize(expected), normalize(actualYaml));

    reporter.assertProblem(
        "Passwords are empty for all migrated users. Each user must reset their password or an admin must set them manually.");
    reporter.assertNoMoreProblems();
  }

  private String normalize(String input) {
    if (input == null) return "";
    return input.trim().replace("\r\n", "\n");
  }

  private Users loadUsers(String path) throws Exception {
    var node = DocNode.wrap(DocReader.json().read(loadResourceAsString(path)));
    return Users.parse(node, Parser.Context.get());
  }

  private String loadResourceAsString(String path) throws Exception {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return new String(in.readAllBytes());
    }
  }

  private Migrator.MigrationContext createContext(Optional<Users> users) {
    return new Migrator.MigrationContext(
        Optional.empty(), Optional.empty(), users, Optional.empty(), Optional.empty());
  }
}
