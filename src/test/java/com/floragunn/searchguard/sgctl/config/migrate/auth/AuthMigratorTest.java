package com.floragunn.searchguard.sgctl.config.migrate.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator.MigrationContext;
import com.floragunn.searchguard.sgctl.config.migrator.AssertableMigrationReporter;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests for {@link AuthMigrator}. */
class AuthMigratorTest {

  private static final Logger logger = LoggerFactory.getLogger(AuthMigratorTest.class);

  @Test
  void testMigrateNativeOnly() throws Exception {
    assertMigrationOutput("native_only");
  }

  @Test
  void testMigrateFileOnly() throws Exception {
    assertMigrationOutput("file_only");
  }

  @Test
  void testMigrateLdapBasic() throws Exception {
    assertMigrationOutput("ldap_basic");
  }

  @Test
  void testMigrateLdapWithScopes() throws Exception {
    assertMigrationOutput("ldap_with_scopes");
  }

  @Test
  void testMigrateLdapInconvertibleScopes() throws Exception {
    assertMigrationOutput(
        "ldap_inconvertible_scope",
        pair -> {
          final AssertableMigrationReporter reporter = pair.getLeft();
          final XPackElasticsearchConfig config = pair.getRight();

          final var realms = config.security().get().authc().getValue().realms().get();
          final var ldap_scoped =
              (XPackElasticsearchConfig.Realm.LdapRealm)
                  realms.get("ldap").get().get("ldap_scoped").get();
          final var group_search_scope = ldap_scoped.groupSearchScope();
          reporter.assertInconvertible(group_search_scope);
        });
  }

  @Test
  void testMigrateLdapWithoutGroupSearch() throws Exception {
    assertMigrationOutput("ldap_without_group_search");
  }

  @Test
  void testMigrateActiveDirectoryOnly() throws Exception {
    assertMigrationOutput("active_directory_only");
  }

  @Test
  void testMigrateUnsupportedRealm() throws Exception {
    var config = loadConfig("/xpack_migrate/elasticsearch/auth/unsupported_realm.yml");
    var context = createContext(Optional.of(config));

    assertThrows(
        UnsupportedOperationException.class, () -> new AuthMigrator().migrate(context, logger));
  }

  @Test
  void testMigrateEmptyElasticsearchConfig() {
    var context = createContext(Optional.empty());
    var reporter = new AssertableMigrationReporter();

    var result = new AuthMigrator().migrate(context, reporter);
    reporter.assertProblem("Skipping auth migration: no elasticsearch configuration provided");

    assertTrue(result.isEmpty());
  }

  @Test
  void testMigrateMultipleRealms() throws Exception {
    assertMigrationOutput("multiple_realms");
  }

  @Test
  void testMigrateRealmOrder() throws Exception {
    assertMigrationOutput("order_test");
  }

  // Helper methods

  private void assertMigrationOutput(String testCaseName) throws Exception {
    assertMigrationOutput(testCaseName, _t -> {});
  }

  private void assertMigrationOutput(
      String testCaseName,
      Consumer<Pair<AssertableMigrationReporter, XPackElasticsearchConfig>> reportChecker)
      throws Exception {
    var inputPath = "/xpack_migrate/elasticsearch/auth/" + testCaseName + ".yml";
    var expectedPath = "/xpack_migrate/expected/auth/" + testCaseName + ".yml";

    var sgAuthC = migrate(inputPath, reportChecker);
    var actualYaml = DocWriter.yaml().writeAsString(sgAuthC.toBasicObject());
    var expectedYaml = loadResourceAsString(expectedPath);

    assertEquals(expectedYaml, actualYaml, "Migration output for " + testCaseName);
  }

  private SgAuthC migrate(
      String path,
      Consumer<Pair<AssertableMigrationReporter, XPackElasticsearchConfig>> reportChecker)
      throws Exception {
    var config = loadConfig(path);
    var context = createContext(Optional.of(config));
    var reporter = new AssertableMigrationReporter();

    var result = new AuthMigrator().migrate(context, reporter);
    reportChecker.accept(Pair.of(reporter, config));
    reporter.assertNoMoreProblems();

    assertEquals(1, result.size());
    var sgAuthC = assertInstanceOf(SgAuthC.class, result.get(0));
    assertEquals("sg_authc.yml", sgAuthC.getFileName());
    return sgAuthC;
  }

  private MigrationContext createContext(Optional<XPackElasticsearchConfig> config) {
    return new MigrationContext(
        Optional.empty(), Optional.empty(), Optional.empty(), config, Optional.empty());
  }

  private XPackElasticsearchConfig loadConfig(String path) throws Exception {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      var node = DocNode.wrap(DocReader.yaml().read(in));
      return XPackElasticsearchConfig.parse(node, Parser.Context.get());
    }
  }

  private String loadResourceAsString(String path) throws IOException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
