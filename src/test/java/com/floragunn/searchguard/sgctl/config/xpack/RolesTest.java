package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Format;
import com.floragunn.codova.validation.ConfigValidationException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RolesTest {

  private static DocNode testCases;

  // Loads all test data from external JSON
  @BeforeAll
  static void loadRolesTests() throws IOException {
    String jsonPath = "/xpack_migrate/roles/roles_test_cases.json";

    try (InputStream is = RolesTest.class.getResourceAsStream(jsonPath)) {
      assertNotNull(is, "Test Cases not found!");
      testCases = DocNode.parse(Format.JSON).from(is);
    } catch (DocumentParseException e) {
      throw new RuntimeException(e);
    }
  }

  // Tests parsing a fully populated role
  @Test
  void testRoleParsing() throws Exception {

    DocNode docNode = testCases.getAsNode("complex_role_case");
    assertNotNull(docNode, "complex_role_case not found!");

    Roles result = Roles.parse(docNode, null);

    assertNotNull(result);
    assertTrue(result.roles().containsKey("complex_role"));

    var role = result.roles().get("complex_role");

    // General asserts
    assertEquals("user1", role.runAs().get(0));
    assertEquals("monitor", role.cluster().get(0));
    assertTrue(role.global().isPresent());
    assertEquals("test2", role.global().get().getAsString("test1"));

    // Indices
    assertEquals("admin", role.indices().get(0).names().get(0));
    assertEquals("read", role.indices().get(0).privileges().get(0));
    assertTrue(role.indices().get(0).allowRestrictedIndices());

    // Applications
    assertEquals("kibana", role.applications().get(0).application());
    assertEquals("read", role.applications().get(0).privileges().get(0));
    assertEquals("*", role.applications().get(0).resources().get(0));

    // Remote indices
    assertEquals("test-cluster1", role.remoteIndices().get(0).clusters().get(0));
    assertEquals("boss2", role.remoteIndices().get(0).names().get(0));
    assertEquals("read", role.remoteIndices().get(0).privileges().get(0));

    // Remote cluster
    assertEquals("test-cluster2", role.remoteCluster().get(0).clusters().get(0));
    assertEquals("all", role.remoteCluster().get(0).privileges().get(0));

    // Rest
    assertTrue(role.description().isPresent());
    assertEquals("Beautiful description", role.description().get());
  }

  // Tests parsing a role, missing all optional fields
  @Test
  void testSmallRoleParsing() throws Exception {

    DocNode docNode = testCases.getAsNode("minimal_role_case");
    assertNotNull(docNode, "minimal_role_case not found!");
    Roles result = Roles.parse(docNode, null);

    var role = result.roles().get("minimal_role");

    assertEquals("all", role.cluster().get(0));
    assertTrue(role.indices().isEmpty());
    assertTrue(role.applications().isEmpty());
    assertTrue(role.runAs().isEmpty());
    assertTrue(role.description().isEmpty());
  }

  // Tests if an empty document will throw an exception
  @Test
  void testEmptyDocumentParsing() throws Exception {
    DocNode docNode = testCases.getAsNode("empty_case");
    assertNotNull(docNode, "empty_case not found!");
    Roles result = Roles.parse(docNode, null);

    assertNotNull(result);
    assertTrue(result.roles().isEmpty());
  }

  @Test
  void testMissingRequiredFields() {
    DocNode docNode = testCases.getAsNode("missing_required_fields_case");

    assertNotNull(docNode, "missing_required_fields_case not found!");

    assertThrows(ConfigValidationException.class, () -> Roles.parse(docNode, null));
  }

  // Tests if a missing empty nested required field throws an exception
  @Test
  void testMissingNestedRequiredField() {

    DocNode docNode = testCases.getAsNode("missing_nested_required_field_case");
    assertNotNull(docNode, "missing_nested_required_field_case not found!");

    assertThrows(ConfigValidationException.class, () -> Roles.parse(docNode, null));
  }
}
