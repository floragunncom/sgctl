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
    assertEquals("user1", role.getValue().runAs().getValue().get(0).get());
    assertEquals("monitor", role.getValue().cluster().get().get(0).get());
    assertTrue(role.getValue().global().get().isPresent());
    assertEquals("test2", role.getValue().global().getValue().getAsString("test1"));

    // Indices
    assertEquals("admin", role.getValue().indices().get().get(0).get().names().get().get(0).get());
    assertEquals(
        "read", role.getValue().indices().get().get(0).get().privileges().get().get(0).get());
    assertTrue(role.getValue().indices().get().get(0).get().allowRestrictedIndices().get());

    // Applications
    assertEquals("kibana", role.getValue().applications().get().get(0).get().application().get());
    assertEquals(
        "read", role.getValue().applications().get().get(0).get().privileges().get().get(0).get());
    assertEquals(
        "*", role.getValue().applications().get().get(0).get().resources().get().get(0).get());

    // Remote indices
    assertEquals(
        "test-cluster1",
        role.getValue().remoteIndices().getValue().get(0).get().clusters().get().get(0).get());
    assertEquals(
        "boss2",
        role.getValue().remoteIndices().getValue().get(0).get().names().get().get(0).get());
    assertEquals(
        "read",
        role.getValue().remoteIndices().getValue().get(0).get().privileges().get().get(0).get());

    // Remote cluster
    assertEquals(
        "test-cluster2",
        role.getValue().remoteCluster().getValue().get(0).get().clusters().get().get(0).get());
    assertEquals(
        "all",
        role.getValue().remoteCluster().getValue().get(0).get().privileges().get().get(0).get());

    // Rest
    assertTrue(role.getValue().description().get().isPresent());
    assertEquals("Beautiful description", role.getValue().description().getValue());
  }

  // Tests parsing a role, missing all optional fields
  @Test
  void testSmallRoleParsing() throws Exception {

    DocNode docNode = testCases.getAsNode("minimal_role_case");
    assertNotNull(docNode, "minimal_role_case not found!");
    Roles result = Roles.parse(docNode, null);

    var role = result.roles().get("minimal_role");

    assertEquals("all", role.getValue().cluster().get().get(0).get());
    assertTrue(role.getValue().indices().get().isEmpty());
    assertTrue(role.getValue().applications().get().isEmpty());
    assertTrue(role.getValue().runAs().get().isEmpty());
    assertTrue(role.getValue().description().get().isEmpty());
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
