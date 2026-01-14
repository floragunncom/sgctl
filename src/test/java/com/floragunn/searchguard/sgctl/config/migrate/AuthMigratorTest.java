package com.floragunn.searchguard.sgctl.config.migrate;

import static com.floragunn.searchguard.sgctl.testutil.TextAssertions.assertEqualsNormalized;
import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator.MigrationContext;
import com.floragunn.searchguard.sgctl.config.migrator.AssertableMigrationReporter;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
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
        "ldap_base_scope",
        reporter -> {
          System.out.println(reporter.generateReport());
          reporter.assertInconvertible(
              "elasticsearch.yml: xpack.security.authc.realms.ldap.ldap1.user_search.scope",
              "These other migratable search scopes DO exist in Search Guard: SUB, ONE. The search scope was omitted from the output because of this.");
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

  @Test
  void testMigrateLdapBothPasswordsSet() throws Exception {
    assertMigrationOutput(
        "ldap_both_passwords",
        reporter -> {
          reporter.assertProblem(
              "elasticsearch.yml: xpack.security.authc.realms.ldap.ldap1.bind_password",
              "Both bind_password and secure_bind_password are set; using secure_bind_password");
        });
  }

  @Test
  void testMigrateLdapPoolDisabled() throws Exception {
    assertMigrationOutput(
        "ldap_pool_disabled",
        reporter -> {
          reporter.assertInconvertible(
              "elasticsearch.yml: xpack.security.authc.realms.ldap.ldap1.user_search.pool.enabled",
              "Connection pool cannot be disabled in Search Guard");
        });
  }

  @Test
  void testMigrateSamlBasic() throws Exception {
    assertFrontendMigrationOutput("saml_basic");
  }

  @Test
  void testMigrateSamlWithNative() throws Exception {
    var inputPath = "/xpack_migrate/elasticsearch/auth/saml_with_native.yml";
    var config = loadConfig(inputPath);
    var context = createContext(Optional.of(config));
    var reporter = new AssertableMigrationReporter();

    var result = new AuthMigrator().migrate(context, reporter);
    reporter.assertNoMoreProblems();

    assertEquals(2, result.size(), "Should return both sg_authc.yml and sg_frontend_authc.yml");

    // Find and verify SgAuthC
    var sgAuthC =
        result.stream()
            .filter(c -> c instanceof SgAuthC)
            .map(c -> (SgAuthC) c)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected SgAuthC in result"));
    assertEquals("sg_authc.yml", sgAuthC.getFileName());
    var expectedAuthcYaml =
        loadResourceAsString("/xpack_migrate/expected/auth/saml_with_native_authc.yml");
    var actualAuthcYaml = DocWriter.yaml().writeAsString(sgAuthC.toBasicObject());
    assertEqualsNormalized(expectedAuthcYaml, actualAuthcYaml, "SgAuthC migration output");

    // Find and verify SgFrontendAuthC
    var sgFrontendAuthC =
        result.stream()
            .filter(c -> c instanceof SgFrontendAuthC)
            .map(c -> (SgFrontendAuthC) c)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected SgFrontendAuthC in result"));
    assertEquals("sg_frontend_authc.yml", sgFrontendAuthC.getFileName());
    var expectedFrontendYaml =
        loadResourceAsString("/xpack_migrate/expected/auth/saml_with_native_frontend.yml");
    var actualFrontendYaml = DocWriter.yaml().writeAsString(sgFrontendAuthC.toBasicObject());
    assertEqualsNormalized(
        expectedFrontendYaml, actualFrontendYaml, "SgFrontendAuthC migration output");
  }

  // Helper methods

  private void assertMigrationOutput(String testCaseName) throws Exception {
    assertMigrationOutput(testCaseName, reporter -> {});
  }

  private void assertMigrationOutput(
      String testCaseName, Consumer<AssertableMigrationReporter> problemAssertions)
      throws Exception {
    var inputPath = "/xpack_migrate/elasticsearch/auth/" + testCaseName + ".yml";
    var expectedPath = "/xpack_migrate/expected/auth/" + testCaseName + ".yml";

    var config = loadConfig(inputPath);
    var context = createContext(Optional.of(config));
    var reporter = new AssertableMigrationReporter();

    var result = new AuthMigrator().migrate(context, reporter);
    problemAssertions.accept(reporter);
    reporter.assertNoMoreProblems();

    assertEquals(1, result.size());
    var sgAuthC = assertInstanceOf(SgAuthC.class, result.get(0));
    assertEqualsNormalized("sg_authc.yml", sgAuthC.getFileName());

    var actualYaml = DocWriter.yaml().writeAsString(sgAuthC.toBasicObject());
    var expectedYaml = loadResourceAsString(expectedPath);

    assertEqualsNormalized(expectedYaml, actualYaml, "Migration output for " + testCaseName);
  }

  private void assertFrontendMigrationOutput(String testCaseName) throws Exception {
    var inputPath = "/xpack_migrate/elasticsearch/auth/" + testCaseName + ".yml";
    var expectedPath = "/xpack_migrate/expected/auth/" + testCaseName + ".yml";

    var sgFrontendAuthC = migrateFrontend(inputPath);
    var actualYaml = DocWriter.yaml().writeAsString(sgFrontendAuthC.toBasicObject());
    var expectedYaml = loadResourceAsString(expectedPath);

    assertEqualsNormalized(
        expectedYaml, actualYaml, "Frontend migration output for " + testCaseName);
  }

  private SgFrontendAuthC migrateFrontend(String path) throws Exception {
    var config = loadConfig(path);
    var context = createContext(Optional.of(config));
    var reporter = new AssertableMigrationReporter();

    var result = new AuthMigrator().migrate(context, reporter);
    reporter.assertNoMoreProblems();

    assertEquals(1, result.size());
    var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));
    assertEquals("sg_frontend_authc.yml", sgFrontendAuthC.getFileName());
    return sgFrontendAuthC;
  }

  private MigrationContext createContext(Optional<XPackElasticsearchConfig> config) {
    return new MigrationContext(
        Optional.empty(), Optional.empty(), Optional.empty(), config, Optional.empty());
  }

  private XPackElasticsearchConfig loadConfig(String path) throws Exception {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      var node = DocNode.wrap(DocReader.yaml().read(in));
      var src = new Source.Config("elasticsearch.yml");
      return TraceableDocNode.parse(node, src, XPackElasticsearchConfig::parse);
    }
  }

  private String loadResourceAsString(String path) throws IOException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
